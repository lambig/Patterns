"""Check public signatures, dependency metadata and a client compiled against 1.3.1."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import urllib.request
from zipfile import ZipFile
import xml.etree.ElementTree as ET

candidate = Path(sys.argv[1]).resolve()
work = Path('build/compatibility').resolve()
work.mkdir(parents=True, exist_ok=True)
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
dependencies = ET.parse(candidate.with_suffix('.pom')).findall('.//m:dependency', ns)
assert [[d.find('m:' + k, ns).text for k in ('groupId', 'artifactId', 'version', 'scope')] for d in dependencies] == [
    ['io.github.lambig', 'Tuplite', '1.0.1', 'compile']]
module = json.loads(candidate.with_suffix('.module').read_text())
for variant in module['variants']:
    if variant['name'] in ('apiElements', 'runtimeElements'):
        assert variant['attributes']['org.gradle.jvm.version'] == 11
        assert variant['dependencies'] == [{'group': 'io.github.lambig', 'module': 'Tuplite', 'version': {'requires': '1.0.1'}}]
    for f in variant['files']:
        artifact = candidate.parent / f['url']
        data = artifact.read_bytes()
        assert f['size'] == len(data)
        for alg in ('sha512', 'sha256', 'sha1', 'md5'):
            if alg in f:
                assert hashlib.new(alg, data).hexdigest() == f[alg]

def download(artifact, version):
    name = f'{artifact}-{version}.jar'
    url = f'https://repo.maven.apache.org/maven2/io/github/lambig/{artifact}/{version}/{name}'
    target = work / name
    target.write_bytes(urllib.request.urlopen(url, timeout=60).read())
    assert hashlib.sha1(target.read_bytes()).hexdigest() == urllib.request.urlopen(url + '.sha1', timeout=60).read().decode().strip()
    return target

baseline = download('Patterns', '1.3.1')
old_tuple = download('Tuplite', '1.0.0')
old_fie = download('functional-interface-extension', '1.1.1')
new_tuple = download('Tuplite', '1.0.1')
new_fie = download('functional-interface-extension', '1.1.3')
java_home = Path(os.environ['JAVA_HOME'])
runtime = Path(os.environ.get('TEST_JAVA_HOME', os.environ['JAVA_HOME'])) / 'bin/java'

def classes(jar):
    with ZipFile(jar) as archive:
        names = sorted(name for name in archive.namelist() if name.endswith('.class'))
        for name in names:
            assert int.from_bytes(archive.read(name)[6:8], 'big') == 55, name
    return [name[:-6].replace('/', '.') for name in names]

types = classes(baseline)
assert types == classes(candidate)
for name in types:
    apis = []
    for label, jar in [('baseline', baseline), ('candidate', candidate)]:
        api = subprocess.check_output([str(java_home / 'bin/javap'), '-public', '-s', '-classpath', str(jar), name], text=True)
        (work / (name + '-' + label + '-api.txt')).write_text(api, encoding='utf-8')
        apis.append(sorted(line.strip() for line in api.splitlines() if not line.startswith('Compiled from')))
    assert apis[0] == apis[1], name + ': public signature changed'

client = work / 'client'
client.mkdir(exist_ok=True)
source = work / 'OldClient.java'
source.write_text('''import io.github.lambig.patterns.*;
import io.github.lambig.tuplite._2.Tuple2;
import java.util.*;
import java.util.function.*;
public class OldClient {
  public static void main(String[] args) {
    Tuple2<Predicate<Integer>, Function<Integer,String>> rule = Patterns.when(n -> n > 0, Patterns.then("positive"));
    Patterns<Integer,String> p = new Patterns<>(List.of(rule));
    if (!p.apply(1).equals("positive") || p.getOptionally(0).isPresent()
        || !p.orElse("zero").apply(0).equals("zero")) throw new AssertionError();
    List<Integer> seen = new ArrayList<>();
    ConsumingPatterns<Integer> c = ConsumingPatterns.of(ConsumingPatterns.when(n -> n > 0, seen::add));
    c.accept(1);
    c.orElseDo(seen::add).accept(0);
    if (!seen.equals(List.of(1,0))) throw new AssertionError();
    Throwable cause = new IllegalArgumentException("reason");
    NoSuchPatternException e = new NoSuchPatternException(cause);
    if (!"reason".equals(e.getMessage()) || e.getCause() != cause) throw new AssertionError();
    NoSuchPatternException empty = new NoSuchPatternException();
    empty.initCause(cause);
    if (empty.getCause() != cause) throw new AssertionError();
    System.out.println("Old compiled client: OK");
  }
}''', encoding='utf-8')
subprocess.run([str(java_home / 'bin/javac'), '--release', '11', '-cp', os.pathsep.join(map(str, (baseline, old_tuple, old_fie))), '-d', str(client), str(source)], check=True)
for libs in [(baseline, old_tuple, old_fie), (baseline, new_tuple, new_fie), (candidate, new_tuple, new_fie)]:
    subprocess.run([str(runtime), '-cp', os.pathsep.join(map(str, (client, *libs))), 'OldClient'], check=True)
print(f'Public signatures preserved: {len(types)} classes; Java 11 bytecode=55')
print(f'Baseline SHA256: {hashlib.sha256(baseline.read_bytes()).hexdigest()}')
print('Old client passed; POM/module export Tuplite 1.0.1; module file hashes match')
