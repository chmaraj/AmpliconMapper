# AmpliconMapper

GUI for mapping amplicon sequence reads to a reference

Offers a GUI for the sake of in-house analysis of raw sequence files (fastq or fastq.gz) from Illumina or Ion Torrent sequencing services. Multiple options are available: Run FastQC, Quick Run, Map IonTorrent Reads, Map Illumina Reads, or View in Tablet.

# Dependencies:

Software:
- Java
- FastQC (https://www.bioinformatics.babraham.ac.uk/projects/fastqc/)
- BBTools (https://jgi.doe.gov/data-and-tools/bbtools/)
- Picard Tools (https://broadinstitute.github.io/picard/)
- Tablet (https://ics.hutton.ac.uk/tablet/)

*Run FastQC*:  
Input a directory containing raw read files, input a directory to output files to, and a number of threads to use.
Returns html files per sample with the QC statistics of associated reads. 

*Quick Run*:  
Input a directory containing raw read files, a directory to output files to, a reference file to map to, and a number of threads to use. Runs BBMap on raw reads for extremely crude mappings. Runs very quickly, but will return very crude mapping indices. 

*Map IonTorrent Reads/Map Illumina Reads*:    
Input a directory containing raw read files, a reference to map to, a directory to output files to, a custom primer file, and a number of threads to use. Runs a complete pipeline which first runs FastQC on raw reads, then trims adapters and custom primers from the amplicon PCR (and merges them if selected as an option in Run Illumina Reads). Reads are subsequently mapped to the reference, formatted, and displayed in Tablet. 

*View in Tablet*
Does exactly as the name suggests. Takes as input a mapped file and the associated reference used, and displays the corresponding mapping in Tablet.

# Known Issues

Due to the programming of BBTools Suite and its internal coding, the linux version of this application requires that inputs provided by the user must have absolute paths that lack any whitespace. If the input paths (reads, references, etc.) contain whitespace, the full pipeline will not run properly.

