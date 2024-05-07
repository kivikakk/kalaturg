SV_IN = Top.sv
PCF_IN = Top.pcf

BASENAME = Top
BUILD_PREFIX = build/
ARTIFACT_PREFIX = $(BUILD_PREFIX)$(BASENAME)

CXXSIM_EXE = $(BUILD_PREFIX)cxxsim
CXXSIM_CC = $(ARTIFACT_PREFIX).cc
CXXSIM_SRCS = $(CXXSIM_CC) $(wildcard cxxsim/*.cc)
CXXSIM_OBJS = $(subst cxxsim/,$(BUILD_PREFIX),$(patsubst %.cc,%.o,$(CXXSIM_SRCS)))

.PHONY: ice40-prog cxxsim clean

all:
	@echo "Targets:"
	@echo "  make ice40"
	@echo "  make ice40-prog"
	@echo "  make cxxsim"
	@echo "  make clean"

clean:
	-rm build/*

ice40: $(ARTIFACT_PREFIX).bin
	
ice40-prog: $(ARTIFACT_PREFIX).bin
	iceprog $<

$(ARTIFACT_PREFIX).bin: $(ARTIFACT_PREFIX).asc
	icepack $< $@

$(ARTIFACT_PREFIX).asc: $(ARTIFACT_PREFIX).json $(PCF_IN)
	nextpnr-ice40 -q --log $(ARTIFACT_PREFIX).tim \
		--up5k --package sg48 \
		--json $(ARTIFACT_PREFIX).json \
		--pcf $(PCF_IN) \
		--asc $@

$(ARTIFACT_PREFIX).json: $(SV_IN)
	yosys -q -g -l $(ARTIFACT_PREFIX).rpt -p ' \
		read_verilog -sv $< ;\
		synth_ice40 -top top ;\
		write_json $@ ;\
	'

cxxsim: $(CXXSIM_EXE)
	$<

$(CXXSIM_EXE): $(CXXSIM_CC) $(CXXSIM_OBJS)

$(CXXSIM_EXE):
	$(CXX) $(OPT_ARGS) $(CXXSIM_OBJS) -o $@

$(BUILD_PREFIX)%.o: %.cc
	echo $< $@

$(CXXSIM_CC): $(SV_IN)
	yosys -q -g -l $(ARTIFACT_PREFIX)-cxxsim.rpt -p '\
		read_verilog -sv $< ;\
		write_cxxrtl -header $@ ;\
	'
