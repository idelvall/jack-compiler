Proposed project grader patch to support package-structured java submissions.

This Makefile supports an additional specification of the main class via the `lang.txt`

1. **Original behavior:** Specify `java` in `lang.txt`, place all classes in the root package and use a fixed name for the main class.
2. **Additional bew behavior:**  Specify `java:your.main.Class` in `lang.txt`, and strucuture the package folders accordingly.

Original discussion thread: https://www.coursera.org/learn/nand2tetris2/discussions/weeks/5/threads/8ANlAA1tEeeQeQo2lD9-LA
