{
    inputs = {
        nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
        flake-utils.url = "github:numtide/flake-utils";
    };

    outputs = { self, nixpkgs, flake-utils }:
        flake-utils.lib.eachDefaultSystem (system:
            let
                pkgs = import nixpkgs { inherit system; };
            in
            {
                devShells.default = pkgs.mkShell rec {
                    buildInputs = with pkgs; [
                        jetbrains.jdk
                    ];
                    nativeBuildInputs = with pkgs.buildPackages; [
                        jetbrains.jdk
                        git
                        maven
                    ];

                    LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath buildInputs;
                    JAVA_HOME = pkgs.jetbrains.jdk;
                };
            }
        );
}
