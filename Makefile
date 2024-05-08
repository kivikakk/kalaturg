BASENAME = Top
BUILD_DIR = build
ARTIFACT_PREFIX = $(BUILD_DIR)/$(BASENAME)

SCALA_SRCS = $(shell find src/main/scala)

CXXSIM_EXE = $(BUILD_DIR)/cxxsim
CXXSIM_CC = $(ARTIFACT_PREFIX).cc
CXXSIM_SRCS = $(CXXSIM_CC) $(wildcard cxxsim/*.cc)
CXXSIM_OBJS = $(subst cxxsim/,$(BUILD_DIR)/,$(patsubst %.cc,%.o,$(CXXSIM_SRCS)))
CXXSIM_OPTS = -std=c++17 -stdlib=libc++ -g -Wall -pedantic -Wno-zero-length-array
# -O3 makes a huge difference to running time.
# -fsanitize=address -fno-omit-frame-pointer for extra de🐞.

.PHONY: ice40-prog cxxsim clean

all:
	@echo "Targets:"
	@echo "  make ice40"
	@echo "  make ice40-prog"
	@echo "  make cxxsim"
	@echo "  make clean"

clean:
	-rm build/*

$(BASENAME)-%.sv: $(SCALA_SRCS)
	sbt run

ice40: $(ARTIFACT_PREFIX).bin
	
ice40-prog: $(ARTIFACT_PREFIX).bin
	iceprog $<

$(ARTIFACT_PREFIX).bin: $(ARTIFACT_PREFIX).asc
	icepack $< $@

$(ARTIFACT_PREFIX).asc: $(ARTIFACT_PREFIX).json $(BASENAME)-ice40.pcf
	nextpnr-ice40 -q --log $(ARTIFACT_PREFIX).tim \
		--up5k --package sg48 \
		--json $(ARTIFACT_PREFIX).json \
		--pcf $(BASENAME)-ice40.pcf \
		--asc $@

$(ARTIFACT_PREFIX).json: $(BASENAME)-ice40.sv
	@mkdir -p $(BUILD_DIR)
	yosys -q -g -l $(ARTIFACT_PREFIX).rpt \
		-p 'read_verilog -sv $<' \
		-p 'synth_ice40 -top top' \
		-p 'write_json $@'

cxxsim: $(CXXSIM_EXE)
	$<

$(CXXSIM_EXE): $(CXXSIM_CC) $(CXXSIM_OBJS)

$(CXXSIM_EXE):
	$(CXX) $(CXXSIM_OPTS) $(CXXSIM_OBJS) -o $@

$(BUILD_DIR)/%.o: cxxsim/%.cc
	@mkdir -p $(BUILD_DIR)
	$(CXX) $(CXXSIM_OPTS) -DCLOCK_NAME=clock \
		-I$(BUILD_DIR) \
		-I$(shell yosys-config --datdir)/include/backends/cxxrtl/runtime \
		-c $< -o $@

# HACK: duplicate above for BUILD_DIR.
$(BUILD_DIR)/%.o: $(BUILD_DIR)/%.cc
	@mkdir -p $(BUILD_DIR)
	$(CXX) $(CXXSIM_OPTS) -DCLOCK_NAME=clock \
		-I$(BUILD_DIR) \
		-I$(shell yosys-config --datdir)/include/backends/cxxrtl/runtime \
		-c $< -o $@


$(CXXSIM_CC): $(BASENAME)-cxxrtl.sv
	@mkdir -p $(BUILD_DIR)
	yosys -q -g -l $(ARTIFACT_PREFIX)-cxxsim.rpt \
		-p 'read_verilog -sv $<' \
		-p 'write_cxxrtl -header $@'
