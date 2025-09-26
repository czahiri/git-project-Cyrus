# GP-2.1 Repository Initialization

## How to run
1) Compile: `javac Git.java`
2) Run: `java Git`

## What it does
- Creates a `git` directory if missing
- Creates `git/objects` if missing
- Creates `git/index` if missing
- Creates `git/HEAD` if missing
- Prints `Git Repository Created` if anything was created
- Prints `Git Repository Already Exists` if all items already exist

### Testing (GP-2.1.1)

How to run:
1. Compile: `Git.java GitTester.java`
2. Run: `java GitTester`
3. Optionally give a number for how many cycles: `java GitTester 5`

What it does:
- Verifies that git/, git/objects, git/index, git/HEAD exist after initialization
- Cleans up by deleting the git folder
- Runs multiple cycles of init + cleanup to check reliability

# GP-2.2 Create a Hash Function

## Overview
This milestone adds a SHA-1 hashing function that generates a unique 40-character hex string for any file's contents. The hash acts as an identifier for the file, similar to how Git tracks file objects.

## How to Run
1. Compile:
   Git.java
2. Run:
   java Git
3. The program will initialize the repository and also print the SHA-1 hash of git/index.

## Implementation
- Method: sha1FromFile(File file)
- Reads the file with FileInputStream
- Updates a MessageDigest with SHA-1 algorithm
- Converts the result into a 40-character hexadecimal string

## Verification
The output hash was checked against the online SHA-1 tool:
https://emn178.github.io/online-tools/sha1.html

# GP-2.3 Create BLOB Files

## What it does
- Computes the SHA-1 hash of the source file's contents.
- Creates a file in `git/objects/` named exactly the 40-char SHA-1.
- Stores an exact copy of the source file's bytes in that object file.

## How to run
1) Compile:
   Blob.java BlobDriver.java
2) Create a sample file:
   echo "hello" > hello.txt
3) Create a BLOB:
   java BlobDriver hello.txt
4) Check:
   ls git/objects
   The printed hash file should exist in `git/objects/`.

## Notes
- If `git/objects/` does not exist, it will be created.
- If the BLOB file already exists, it is not recreated.
- Compression toggle is available in `Blob.COMPRESS`. Default is false.