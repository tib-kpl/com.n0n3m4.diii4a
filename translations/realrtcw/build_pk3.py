"""Builds RealRTCW's language pack from the files next to this script.

    pack/     files for every language, at the root of the pack (the menus with the language option)
    fr/       the French files, packed under lang/fr/: the engine reads them first when cl_language is 1

Text files are kept in UTF-8 here; the game reads Latin-1 with Windows line ends, so they are
converted when packed. Other files (images) are packed as they are.

    python build_pk3.py [output.pk3]
"""
import os
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
NAME = 'zz_realrtcw_lang.pk3'
LANGUAGES = ('fr',)
TEXT = ('.txt', '.menu', '.cfg', '.h')


def add_tree(z, root, prefix):
    for d, _, files in sorted(os.walk(root)):
        for f in sorted(files):
            p = os.path.join(d, f)
            rel = prefix + os.path.relpath(p, root).replace(os.sep, '/')
            if f.lower().endswith(TEXT):
                s = open(p, encoding='utf-8').read().replace('\r\n', '\n')
                try:
                    data = s.replace('\n', '\r\n').encode('latin-1')
                except UnicodeEncodeError as e:
                    sys.exit('%s: character outside Latin-1 (the game fonts have no glyph): %r' % (rel, s[e.start]))
            else:
                data = open(p, 'rb').read()
            z.writestr(rel, data)


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, NAME)
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
        add_tree(z, os.path.join(HERE, 'pack'), '')
        for code in LANGUAGES:
            add_tree(z, os.path.join(HERE, code), 'lang/%s/' % code)
    print(out)


if __name__ == '__main__':
    main()
