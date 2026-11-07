"""Compile and execute both complete programs from the README."""
import os
from pathlib import Path
import re
import subprocess
import sys

work = Path('build/readme-example').resolve()
work.mkdir(parents=True, exist_ok=True)
java_home = Path(os.environ['JAVA_HOME'])
runtime = Path(os.environ.get('TEST_JAVA_HOME', os.environ['JAVA_HOME'])) / 'bin/java'
expected = {
    'ClassificationExample': ['large', 'Optional.empty', '[large, small, unknown]'],
    'RoutingExample': ['[large:12, other:0]'],
}
blocks = re.findall(r'```java\n(.*?)```', Path('README.md').read_text(encoding='utf-8'), re.S)
assert len(blocks) == len(expected)
for source in blocks:
    name = re.search(r'public class (\w+)', source).group(1)
    file = work / (name + '.java')
    file.write_text(source, encoding='utf-8')
    subprocess.run([str(java_home / 'bin/javac'), '--release', '11', '-encoding', 'UTF-8', '-cp', sys.argv[1], '-d', str(work), str(file)], check=True)
    result = subprocess.check_output([str(runtime), '-cp', str(work) + os.pathsep + sys.argv[1], name], text=True)
    assert result.splitlines() == expected[name], result
    print(name + ': ' + result.strip().replace('\n', '; '))
