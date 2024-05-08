attribute \cxxrtl_blackbox 1
attribute \blackbox 1
module \CXXRTLTestbench
    attribute \cxxrtl_edge "p"
    wire input 1 \clock

    wire input 2 \rx

    attribute \cxxrtl_sync 1
    wire output 3 \tx
end
 