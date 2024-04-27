{
  inputs = {
    hdx.url = git+https://hrzn.ee/kivikakk/hdx;
    nixpkgs.follows = "hdx/nixpkgs";
    flake-utils.follows = "hdx/flake-utils";
  };

  outputs = inputs @ {
    self,
    hdx,
    nixpkgs,
    flake-utils,
  }:
    flake-utils.lib.eachDefaultSystem (system: let
      pkgs = import nixpkgs {inherit system;};
      inherit (hdx.packages.${system}) rainhdx;
      inherit (rainhdx) python;
    in rec {
      formatter = pkgs.alejandra;

      packages.default = rainhdx.buildProject {
        name = "kalaturg";
        src = ./.;

        nativeBuildInputs = [];
      };

      devShells = packages.default.devShells;
    });
}
