# GP-2.1 Repository Initialization

## How to run
1) Compile: `java Git.java`
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
   Blob.java
2) Create a sample file:
   echo "hello" > hello.txt
3) Create a BLOB:
   java Blob
4) Check:
   ls git/objects
   The printed hash file should exist in `git/objects/`.

## Notes
- If `git/objects/` does not exist, it will be created.
- If the BLOB file already exists, it is not recreated.
- Compression toggle is available in `Blob.COMPRESS`. Default is false.

## Testing (GP-2.3.1)
1) Compile:
   Blob.java BlobTester.java
2) Run:
   java BlobTester
3) The tester creates a source file, creates a BLOB, verifies the object exists, resets the objects directory, verifies removal, then creates it again.

# GP-2.4 Updating the Index File

## What this adds
- Staging support using a plain-text index at `git/index`.
- Each line is exactly `<sha1><space><filename>`.
- If a filename is already in the index, its line is replaced with the new hash.
- No trailing spaces on lines and no extra newline at the end of the file.

## How to use in code
Use the `Index` class from your own Java code

Notes:
- This uses the existing BLOB logic to compute the SHA-1 and store the file.
- The method creates `git/`, `git/objects/`, and `git/index` if they do not exist.

# GP-3.1 Updating the Index Formatting

## What this adds
- Updates the `git/index` file format to store the **relative path** of each staged file instead of just the filename.
- Each entry now has the form `<sha1><space><pathname>`.
- Handles duplicates and modifications:
  - If the same file with the same hash is added again, it is ignored.
  - If identical files exist in different directories, both are tracked with separate paths but the same hash.
  - If a file’s contents change, the index updates with the new hash.

## Example

Before (old format):
0acc46ad73849ea9832f600de83a014c9db9cdf0 Hello.txt

After (new format):
4377a91cdfd44db9a9bbf056849c7da0fc6cc7be myProgram/README.md
ca2cb4da9485e7bbce664bf4f5ee2216a36af4fb myProgram/Hello.txt
0acc46ad73849ea9832f600de83a014c9db9cdf0 myProgram/scripts/Cat.java

## How to run
1) Compile:
   Index.java Blob.java
2) Create some files and directories:
   mkdir -p myProgram/scripts
   echo "hello" > myProgram/Hello.txt
   echo "cat code" > myProgram/scripts/Cat.java
3) Stage the files from Java code:
   Index idx = new Index();
   idx.add("myProgram/Hello.txt");
   idx.add("myProgram/scripts/Cat.java");
4) Check git/index to see updated entries with relative paths.

# GP-3.2 Creating a Basic Tree

## What this adds
- Implements tree creation for directories.
- A tree represents the structure of files and subdirectories inside a folder.
- Each tree is stored as an object in `git/objects` with a unique SHA-1 hash.
- Trees contain:
  - `blob <sha1> <pathname>` entries for files
  - `tree <sha1> <pathname>` entries for subdirectories
- Trees are created recursively so that subdirectories generate their own tree objects, which are then referenced by their parent tree.

## Example
For a directory `myProgram/` with files and a `scripts/` folder:
myProgram/
README.md
Hello.txt
scripts/
Cat.java
goCopy code
The `scripts/` directory produces:


blob 0acc46ad73849ea9832f600de83a014c9db9cdf0 scripts/Cat.java
goCopy code
The `myProgram/` directory tree includes:

blob 4377a91cdfd44db9a9bbf056849c7da0fc6cc7be myProgram/README.md
blob 0a4d55a8d778e5022fab701977c5d840bbc486d0 myProgram/Hello.txt
tree 483b5e082cf5502b303ba3dd4f3469a49495c9ef myProgram/scripts
bashCopy code
The SHA-1 of the tree file becomes its filename inside `git/objects/`.

## How to run
1) Compile:
   Tree.java Blob.java
2) Create a directory and sample files:
   mkdir -p myProgram/scripts
   echo "readme content" > myProgram/README.md
   echo "hello" > myProgram/Hello.txt
   echo "cat code" > myProgram/scripts/Cat.java
3) Run the tree creation from Java code:
   Tree t = new Tree();
   String treeHash = t.createTree("myProgram");
   System.out.println("Tree hash: " + treeHash);
4) Check inside `git/objects/` to verify the tree file exists.

# GP-3.3 Creating a Tree from the Index

## What this adds
- Generates **tree objects directly from the index** instead of scanning directories.
- Uses a **working list** that starts with `blob <sha1> <path>` for each index entry.
- Collapses directories bottom-up into `tree <sha1> <dirname>` entries until only the root tree remains.

## Files
- WorkingList.java — builds trees from index
- WorkingListTester.java — simple runner, builds one root tree and prints its hash and contents

## How to run
1) Compile:
   Blob.java Index.java WorkingList.java WorkingListTester.java
2) Run:
   java WorkingListTester
3) Output:
   - Prints the **root tree SHA-1**
   - Prints the **contents of the root tree** stored under `git/objects/<sha1>`

## No Critical Bugs or Errors found, BUT some functionality missing in create tree - by priscilla
1) I discovered that when creating a tree, the “root” is essentially not as concentrated as it needs to be. Instead of the root tree listing the contents of the main directory, there needs to be one more object made that is named the hash of the contents of the root directory and then contains the name of the root directory.
