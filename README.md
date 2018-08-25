# A sound and scalable method for static data race detection in Java programs

This repository presents the results of my MSc thesis at Amirkabir University of Technology.

The file `main.pdf` contains the main body of the report.

The file `appendix.pdf` contains all the proofs and auxiliary definitions.

The directory `report` contains the Persian version of my MSc thesis.

The directory `src` contains the source code of Camelspider and the benchmark programs.

Camelspider is a fast, scalable, and precise datarace detection tool. The core idea behind Camelspider is to summarize the effect of each function in isolation and use the precomputed summary whenever needed. The particular abstraction used in camelspider allows precise modeling of program behavior which in turn leads to a more precise bug detection.
