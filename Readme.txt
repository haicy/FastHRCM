****************************************************************************                             
	                              FastHRCM
  (Multi-thread Concurrent Compression for Large Collections of Genomes )

     https://github.com/haicy/FastHRCM

          Copyright (C) 2020                  
****************************************************************************

1. Introduction

1.1 FastHRCM is implemented with Java and suggested to be run on Linux operating system.
1.2 BSC executable file should be put in the same directory of the executable jar package (e.g. FastHRCM.jar)
1.3 Set BSC executable permission: chmod 777 bsc

****************************************************************************

2. Use

2.1 Usage
java -jar [-Xmx20g] FastHRCM.jar {file_name.txt} {compress | decompress} [thread_number]
   -[-Xms20g] is the initial allocation of memory, optianal. Please make sure your machine has more than 10GB physical memory.
   -FastHRCM.jar is the compression executable file,required.
   -{file_name.txt} is the list of the reference file path (the first line) and the to-be-compressed file paths, required.
   -{compress | decompress} is mode,  choose one of them according to requirement, required
   -[thread_number] is the size of the thread pool used for compression, default is 4, optional for compression.

2.2 Output:
    1.compressed file named filename.bsc in current directory
    2.decompressed file named filename.fa in the result directory 

****************************************************************************

3. Example

3.1 You can download the executable jar package and the test datasets in the test directory.

3.2 compress and decompress hg17_chr22.fa and hg18_chr22.fa, using hg13_chr22.fa as reference. The paths of reference file and to-be-compressed files are written in chr22.txt in turn.
    java -jar -Xmx20g FastHRCM.jar chr22.txt compress 6
    output: chr22.bsc


    java -jar -Xmx20g FastHRCM.jar chr22.txt decompress
    output: result/hg17_chr22.fa 
	    result/hg18_chr22.fa

3.3 check the difference between original file and decompressed file
    diff hg17_chr22.fa result/hg17_chr22.fa
    diff hg18_chr22.fa result/hg18_chr22.fa

***************************************************************************
