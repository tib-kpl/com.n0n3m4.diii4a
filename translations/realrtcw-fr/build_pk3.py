"""Builds the RealRTCW French translation pack from the text files next to this script.

The files are kept in UTF-8 here; the game reads Latin-1 with Windows line ends, so they are
converted when packed. The pack is named to load after RealRTCW's own packs (z_*.pk3), so its
text/ files replace the English ones.

    python build_pk3.py [output.pk3]
"""
import os
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
NAME = 'zz_realrtcw_fr.pk3'


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, NAME)
    with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
        for d, _, files in os.walk(os.path.join(HERE, 'text')):
            for f in sorted(files):
                p = os.path.join(d, f)
                rel = os.path.relpath(p, HERE).replace(os.sep, '/')
                s = open(p, encoding='utf-8').read().replace('\r\n', '\n')
                try:
                    b = s.replace('\n', '\r\n').encode('latin-1')
                except UnicodeEncodeError as e:
                    sys.exit('%s: character outside Latin-1 (the game fonts have no glyph): %r' % (rel, s[e.start]))
                z.writestr(rel, b)
    print(out)


if __name__ == '__main__':
    main()
