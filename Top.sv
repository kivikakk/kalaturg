// Generated by CIRCT firtool-1.62.0
// external module SB_GB

module RX(
  input        clock,
               reset,
               io_ready,
  output       io_valid,
  output [7:0] io_bits_byte,
  output       io_bits_err,
  input        platIo
);

  reg             syncedPlatIo_REG;
  reg             syncedPlatIo;
  reg             validReg;
  reg  [7:0]      bitsReg_byte;
  reg             bitsReg_err;
  reg  [1:0]      state;
  reg  [10:0]     timerReg;
  reg  [3:0]      counterReg;
  reg  [9:0]      shiftReg;
  wire            _GEN = state == 2'h0;
  wire            _GEN_0 = state == 2'h1;
  wire            _GEN_1 = timerReg == 11'h0;
  wire            _GEN_2 = _GEN_0 & _GEN_1;
  wire            _GEN_3 = state == 2'h2;
  wire            _GEN_4 = _GEN | _GEN_0;
  wire [3:0][1:0] _GEN_5 =
    {{state},
     {2'h0},
     {_GEN_1 & counterReg == 4'h0 ? 2'h2 : state},
     {syncedPlatIo ? state : 2'h1}};
  always @(posedge clock) begin
    if (reset) begin
      syncedPlatIo_REG <= 1'h1;
      syncedPlatIo <= 1'h1;
      validReg <= 1'h0;
      bitsReg_byte <= 8'h0;
      bitsReg_err <= 1'h0;
      state <= 2'h0;
      timerReg <= 11'h0;
      counterReg <= 4'h0;
      shiftReg <= 10'h0;
    end
    else begin
      syncedPlatIo_REG <= platIo;
      syncedPlatIo <= syncedPlatIo_REG;
      if (_GEN_4 | ~_GEN_3)
        validReg <= ~io_ready & validReg;
      else
        validReg <= io_ready | validReg;
      if (_GEN_4 | ~(_GEN_3 & io_ready)) begin
      end
      else begin
        bitsReg_byte <= shiftReg[8:1];
        bitsReg_err <= shiftReg[9] | ~(shiftReg[0]);
      end
      state <= _GEN_5[state];
      if (_GEN) begin
        if (syncedPlatIo) begin
        end
        else begin
          timerReg <= 11'h271;
          counterReg <= 4'h9;
        end
      end
      else begin
        if (_GEN_0) begin
          if (_GEN_1)
            timerReg <= 11'h4E1;
          else
            timerReg <= timerReg - 11'h1;
        end
        if (_GEN_2)
          counterReg <= counterReg - 4'h1;
      end
      if (_GEN | ~_GEN_2) begin
      end
      else
        shiftReg <= {shiftReg[8:0], syncedPlatIo};
    end
  end // always @(posedge)
  assign io_valid = validReg;
  assign io_bits_byte = bitsReg_byte;
  assign io_bits_err = bitsReg_err;
endmodule

// VCS coverage exclude_file
module ram_32x9(
  input  [4:0] R0_addr,
  input        R0_en,
               R0_clk,
  output [8:0] R0_data,
  input  [4:0] W0_addr,
  input        W0_en,
               W0_clk,
  input  [8:0] W0_data
);

  reg [8:0] Memory[0:31];
  reg       _R0_en_d0;
  reg [4:0] _R0_addr_d0;
  always @(posedge R0_clk) begin
    _R0_en_d0 <= R0_en;
    _R0_addr_d0 <= R0_addr;
  end // always @(posedge)
  always @(posedge W0_clk) begin
    if (W0_en & 1'h1)
      Memory[W0_addr] <= W0_data;
  end // always @(posedge)
  assign R0_data = _R0_en_d0 ? Memory[_R0_addr_d0] : 9'bx;
endmodule

module Queue32_RXOut(
  input        clock,
               reset,
  output       io_enq_ready,
  input        io_enq_valid,
  input  [7:0] io_enq_bits_byte,
  input        io_enq_bits_err,
               io_deq_ready,
  output       io_deq_valid,
  output [7:0] io_deq_bits_byte,
  output       io_deq_bits_err
);

  wire [8:0] _ram_ext_R0_data;
  reg  [4:0] enq_ptr_value;
  reg  [4:0] deq_ptr_value;
  reg        maybe_full;
  wire       ptr_match = enq_ptr_value == deq_ptr_value;
  wire       empty = ptr_match & ~maybe_full;
  wire       full = ptr_match & maybe_full;
  wire       do_enq = ~full & io_enq_valid;
  wire       do_deq = io_deq_ready & ~empty;
  always @(posedge clock) begin
    if (reset) begin
      enq_ptr_value <= 5'h0;
      deq_ptr_value <= 5'h0;
      maybe_full <= 1'h0;
    end
    else begin
      if (do_enq)
        enq_ptr_value <= enq_ptr_value + 5'h1;
      if (do_deq)
        deq_ptr_value <= deq_ptr_value + 5'h1;
      if (~(do_enq == do_deq))
        maybe_full <= do_enq;
    end
  end // always @(posedge)
  ram_32x9 ram_ext (
    .R0_addr (do_deq ? ((&deq_ptr_value) ? 5'h0 : deq_ptr_value + 5'h1) : deq_ptr_value),
    .R0_en   (1'h1),
    .R0_clk  (clock),
    .R0_data (_ram_ext_R0_data),
    .W0_addr (enq_ptr_value),
    .W0_en   (do_enq),
    .W0_clk  (clock),
    .W0_data ({io_enq_bits_err, io_enq_bits_byte})
  );
  assign io_enq_ready = ~full;
  assign io_deq_valid = ~empty;
  assign io_deq_bits_byte = _ram_ext_R0_data[7:0];
  assign io_deq_bits_err = _ram_ext_R0_data[8];
endmodule

module TX(
  input        clock,
               reset,
  output       io_ready,
  input        io_valid,
  input  [7:0] io_bits,
  output       platIo
);

  reg         state;
  reg  [10:0] timerReg;
  reg  [3:0]  counterReg;
  reg  [9:0]  shiftReg;
  wire        _GEN = timerReg == 11'h0;
  always @(posedge clock) begin
    if (reset) begin
      state <= 1'h0;
      timerReg <= 11'h0;
      counterReg <= 4'h0;
      shiftReg <= 10'h0;
    end
    else if (state) begin
      state <= ~(state & _GEN & counterReg == 4'h0) & state;
      if (_GEN)
        timerReg <= 11'h4E1;
      else
        timerReg <= timerReg - 11'h1;
      if (state & _GEN) begin
        counterReg <= counterReg - 4'h1;
        shiftReg <= {shiftReg[8:0], 1'h0};
      end
    end
    else begin
      state <= io_valid | state;
      if (io_valid) begin
        timerReg <= 11'h4E1;
        counterReg <= 4'h9;
        shiftReg <= {1'h0, io_bits, 1'h1};
      end
    end
  end // always @(posedge)
  assign io_ready = ~state;
  assign platIo = ~state | ~state | shiftReg[9];
endmodule

// VCS coverage exclude_file
module ram_32x8(
  input  [4:0] R0_addr,
  input        R0_en,
               R0_clk,
  output [7:0] R0_data,
  input  [4:0] W0_addr,
  input        W0_en,
               W0_clk,
  input  [7:0] W0_data
);

  reg [7:0] Memory[0:31];
  reg       _R0_en_d0;
  reg [4:0] _R0_addr_d0;
  always @(posedge R0_clk) begin
    _R0_en_d0 <= R0_en;
    _R0_addr_d0 <= R0_addr;
  end // always @(posedge)
  always @(posedge W0_clk) begin
    if (W0_en & 1'h1)
      Memory[W0_addr] <= W0_data;
  end // always @(posedge)
  assign R0_data = _R0_en_d0 ? Memory[_R0_addr_d0] : 8'bx;
endmodule

module Queue32_UInt8(
  input        clock,
               reset,
  output       io_enq_ready,
  input        io_enq_valid,
  input  [7:0] io_enq_bits,
  input        io_deq_ready,
  output       io_deq_valid,
  output [7:0] io_deq_bits
);

  reg  [4:0] enq_ptr_value;
  reg  [4:0] deq_ptr_value;
  reg        maybe_full;
  wire       ptr_match = enq_ptr_value == deq_ptr_value;
  wire       empty = ptr_match & ~maybe_full;
  wire       full = ptr_match & maybe_full;
  wire       do_enq = ~full & io_enq_valid;
  wire       do_deq = io_deq_ready & ~empty;
  always @(posedge clock) begin
    if (reset) begin
      enq_ptr_value <= 5'h0;
      deq_ptr_value <= 5'h0;
      maybe_full <= 1'h0;
    end
    else begin
      if (do_enq)
        enq_ptr_value <= enq_ptr_value + 5'h1;
      if (do_deq)
        deq_ptr_value <= deq_ptr_value + 5'h1;
      if (~(do_enq == do_deq))
        maybe_full <= do_enq;
    end
  end // always @(posedge)
  ram_32x8 ram_ext (
    .R0_addr (do_deq ? ((&deq_ptr_value) ? 5'h0 : deq_ptr_value + 5'h1) : deq_ptr_value),
    .R0_en   (1'h1),
    .R0_clk  (clock),
    .R0_data (io_deq_bits),
    .W0_addr (enq_ptr_value),
    .W0_en   (do_enq),
    .W0_clk  (clock),
    .W0_data (io_enq_bits)
  );
  assign io_enq_ready = ~full;
  assign io_deq_valid = ~empty;
endmodule

module UART(
  input        clock,
               reset,
  output       txIo_ready,
  input        txIo_valid,
  input  [7:0] txIo_bits,
  input        rxIo_ready,
  output       rxIo_valid,
  output [7:0] rxIo_bits_byte,
  output       rxIo_bits_err,
  input        platIo_rx,
  output       platIo_tx
);

  wire       _tx_io_q_io_deq_valid;
  wire [7:0] _tx_io_q_io_deq_bits;
  wire       _tx_io_ready;
  wire       _rxIo_q_io_enq_ready;
  wire       _rx_io_valid;
  wire [7:0] _rx_io_bits_byte;
  wire       _rx_io_bits_err;
  RX rx (
    .clock        (clock),
    .reset        (reset),
    .io_ready     (_rxIo_q_io_enq_ready),
    .io_valid     (_rx_io_valid),
    .io_bits_byte (_rx_io_bits_byte),
    .io_bits_err  (_rx_io_bits_err),
    .platIo       (platIo_rx)
  );
  Queue32_RXOut rxIo_q (
    .clock            (clock),
    .reset            (reset),
    .io_enq_ready     (_rxIo_q_io_enq_ready),
    .io_enq_valid     (_rx_io_valid),
    .io_enq_bits_byte (_rx_io_bits_byte),
    .io_enq_bits_err  (_rx_io_bits_err),
    .io_deq_ready     (rxIo_ready),
    .io_deq_valid     (rxIo_valid),
    .io_deq_bits_byte (rxIo_bits_byte),
    .io_deq_bits_err  (rxIo_bits_err)
  );
  TX tx (
    .clock    (clock),
    .reset    (reset),
    .io_ready (_tx_io_ready),
    .io_valid (_tx_io_q_io_deq_valid),
    .io_bits  (_tx_io_q_io_deq_bits),
    .platIo   (platIo_tx)
  );
  Queue32_UInt8 tx_io_q (
    .clock        (clock),
    .reset        (reset),
    .io_enq_ready (txIo_ready),
    .io_enq_valid (txIo_valid),
    .io_enq_bits  (txIo_bits),
    .io_deq_ready (_tx_io_ready),
    .io_deq_valid (_tx_io_q_io_deq_valid),
    .io_deq_bits  (_tx_io_q_io_deq_bits)
  );
endmodule

module TopInner(
  input  clock,
         reset,
         io_plat_rx,
  output io_plat_tx,
         io_ledr,
         io_pmod1a1,
         io_pmod1a2,
         io_pmod1a3
);

  wire            _uartM_txIo_ready;
  wire            _uartM_rxIo_valid;
  wire [7:0]      _uartM_rxIo_bits_byte;
  wire            _uartM_rxIo_bits_err;
  reg             ledReg;
  reg  [22:0]     timerReg;
  reg  [7:0]      rgbVecReg_0;
  reg  [7:0]      rgbVecReg_1;
  reg  [7:0]      rgbVecReg_2;
  reg  [13:0]     io_pmod1a1_cntReg;
  reg  [13:0]     io_pmod1a2_cntReg;
  reg  [13:0]     io_pmod1a3_cntReg;
  reg  [14:0]     rgbCounterReg;
  reg  [1:0]      elementIxReg;
  reg             incrementingReg;
  wire [3:0][7:0] _GEN = {{rgbVecReg_0}, {rgbVecReg_2}, {rgbVecReg_1}, {rgbVecReg_0}};
  wire [7:0]      _GEN_0 = _GEN[elementIxReg];
  wire            _GEN_1 = elementIxReg == 2'h1;
  wire            _GEN_2 = elementIxReg == 2'h2;
  wire            _GEN_3 = _GEN_0 != 8'hFF;
  wire [7:0]      _rgbVecReg_T = _GEN_0 + 8'h1;
  wire [7:0]      _rgbVecReg_T_2 = _GEN_0 - 8'h1;
  wire            _GEN_4 = timerReg == 23'h0;
  always @(posedge clock) begin
    if (reset) begin
      ledReg <= 1'h1;
      timerReg <= 23'h2DC6BF;
      rgbVecReg_0 <= 8'hFF;
      rgbVecReg_1 <= 8'h0;
      rgbVecReg_2 <= 8'h0;
      io_pmod1a1_cntReg <= 14'h0;
      io_pmod1a2_cntReg <= 14'h0;
      io_pmod1a3_cntReg <= 14'h0;
      rgbCounterReg <= 15'h0;
      elementIxReg <= 2'h1;
      incrementingReg <= 1'h1;
    end
    else begin
      ledReg <= _GEN_4 ^ ledReg;
      if (_GEN_4)
        timerReg <= 23'h5B8D7F;
      else
        timerReg <= timerReg - 23'h1;
      if (rgbCounterReg == 15'h5B8C) begin
        if (incrementingReg) begin
          if (_GEN_3 & ~(|elementIxReg))
            rgbVecReg_0 <= _rgbVecReg_T;
          if (_GEN_3 & _GEN_1)
            rgbVecReg_1 <= _rgbVecReg_T;
          if (_GEN_3 & _GEN_2)
            rgbVecReg_2 <= _rgbVecReg_T;
          if (~_GEN_3) begin
            if (|elementIxReg)
              elementIxReg <= elementIxReg - 2'h1;
            else
              elementIxReg <= 2'h2;
          end
          incrementingReg <= _GEN_3 ^ ~incrementingReg;
        end
        else begin
          if ((|_GEN_0) & ~(|elementIxReg))
            rgbVecReg_0 <= _rgbVecReg_T_2;
          if ((|_GEN_0) & _GEN_1)
            rgbVecReg_1 <= _rgbVecReg_T_2;
          if ((|_GEN_0) & _GEN_2)
            rgbVecReg_2 <= _rgbVecReg_T_2;
          if (~(|_GEN_0)) begin
            if (|elementIxReg)
              elementIxReg <= elementIxReg - 2'h1;
            else
              elementIxReg <= 2'h2;
          end
          incrementingReg <= (|_GEN_0) ^ ~incrementingReg;
        end
        rgbCounterReg <= 15'h0;
      end
      else
        rgbCounterReg <= rgbCounterReg + 15'h1;
      if (io_pmod1a1_cntReg == 14'h2DC5)
        io_pmod1a1_cntReg <= 14'h0;
      else
        io_pmod1a1_cntReg <= io_pmod1a1_cntReg + 14'h1;
      if (io_pmod1a2_cntReg == 14'h2DC5)
        io_pmod1a2_cntReg <= 14'h0;
      else
        io_pmod1a2_cntReg <= io_pmod1a2_cntReg + 14'h1;
      if (io_pmod1a3_cntReg == 14'h2DC5)
        io_pmod1a3_cntReg <= 14'h0;
      else
        io_pmod1a3_cntReg <= io_pmod1a3_cntReg + 14'h1;
    end
  end // always @(posedge)
  UART uartM (
    .clock          (clock),
    .reset          (reset),
    .txIo_ready     (_uartM_txIo_ready),
    .txIo_valid     (_uartM_txIo_ready & _uartM_rxIo_valid & ~_uartM_rxIo_bits_err),
    .txIo_bits      (_uartM_rxIo_bits_byte),
    .rxIo_ready     (_uartM_txIo_ready),
    .rxIo_valid     (_uartM_rxIo_valid),
    .rxIo_bits_byte (_uartM_rxIo_bits_byte),
    .rxIo_bits_err  (_uartM_rxIo_bits_err),
    .platIo_rx      (io_plat_rx),
    .platIo_tx      (io_plat_tx)
  );
  assign io_ledr = ledReg;
  assign io_pmod1a1 = {6'h0, rgbVecReg_0} * 14'h2D > io_pmod1a1_cntReg;
  assign io_pmod1a2 = {6'h0, rgbVecReg_1} * 14'h2D > io_pmod1a2_cntReg;
  assign io_pmod1a3 = {6'h0, rgbVecReg_2} * 14'h2D > io_pmod1a3_cntReg;
endmodule

module top(
  input  clki,
         io_ubtn,
         io_plat_rx,
  output io_plat_tx,
         io_ledr,
         io_ledg,
         io_pmod1a1,
         io_pmod1a2,
         io_pmod1a3
);

  wire       _clk_gb_GLOBAL_BUFFER_OUTPUT;
  reg  [7:0] resetTimerReg;
  wire       _GEN = resetTimerReg == 8'hB4;
  always @(posedge _clk_gb_GLOBAL_BUFFER_OUTPUT) begin
    if (~_GEN)
      resetTimerReg <= resetTimerReg + 8'h1;
  end // always @(posedge)
  SB_GB clk_gb (
    .USER_SIGNAL_TO_GLOBAL_BUFFER (clki),
    .GLOBAL_BUFFER_OUTPUT         (_clk_gb_GLOBAL_BUFFER_OUTPUT)
  );
  TopInner inner (
    .clock      (_clk_gb_GLOBAL_BUFFER_OUTPUT),
    .reset      (~_GEN | ~io_ubtn),
    .io_plat_rx (io_plat_rx),
    .io_plat_tx (io_plat_tx),
    .io_ledr    (io_ledr),
    .io_pmod1a1 (io_pmod1a1),
    .io_pmod1a2 (io_pmod1a2),
    .io_pmod1a3 (io_pmod1a3)
  );
  assign io_ledg = 1'h0;
endmodule

