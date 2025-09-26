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