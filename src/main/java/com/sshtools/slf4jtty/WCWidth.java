package com.sshtools.slf4jtty;


/*
 * Java implementation of xterms wcwidth - https://fossies.org/linux/xterm/wcwidth.c
 * 
 * ---------------------------------------------------------------------------------
 * 
 * This is an implementation of wcwidth() and wcswidth() (defined in
 * IEEE Std 1002.1-2001) for Unicode.
 *
 * http://www.opengroup.org/onlinepubs/007904975/functions/wcwidth.html
 * http://www.opengroup.org/onlinepubs/007904975/functions/wcswidth.html
 *
 * In fixed-width output devices, Latin characters all occupy a single
 * "cell" position of equal width, whereas ideographic CJK characters
 * occupy two such cells. Interoperability between terminal-line
 * applications and (teletype-style) character terminals using the
 * UTF-8 encoding requires agreement on which character should advance
 * the cursor by how many cell positions. No established formal
 * standards exist at present on which Unicode character shall occupy
 * how many cell positions on character terminals. These routines are
 * a first attempt of defining such behavior based on simple rules
 * applied to data provided by the Unicode Consortium.
 *
 * For some graphical characters, the Unicode standard explicitly
 * defines a character-cell width via the definition of the East Asian
 * FullWidth (F), Wide (W), Half-width (H), and Narrow (Na) classes.
 * In all these cases, there is no ambiguity about which width a
 * terminal shall use. For characters in the East Asian Ambiguous (A)
 * class, the width choice depends purely on a preference of backward
 * compatibility with either historic CJK or Western practice.
 * Choosing single-width for these characters is easy to justify as
 * the appropriate long-term solution, as the CJK practice of
 * displaying these characters as double-width comes from historic
 * implementation simplicity (8-bit encoded characters were displayed
 * single-width and 16-bit ones double-width, even for Greek,
 * Cyrillic, etc.) and not any typographic considerations.
 *
 * Much less clear is the choice of width for the Not East Asian
 * (Neutral) class. Existing practice does not dictate a width for any
 * of these characters. It would nevertheless make sense
 * typographically to allocate two character cells to characters such
 * as for instance EM SPACE or VOLUME INTEGRAL, which cannot be
 * represented adequately with a single-width glyph. The following
 * routines at present merely assign a single-cell width to all
 * neutral characters, in the interest of simplicity. This is not
 * entirely satisfactory and should be reconsidered before
 * establishing a formal standard in this area. At the moment, the
 * decision which Not East Asian (Neutral) characters should be
 * represented by double-width glyphs cannot yet be answered by
 * applying a simple rule from the Unicode database content. Setting
 * up a proper standard for the behavior of UTF-8 character terminals
 * will require a careful analysis not only of each Unicode character,
 * but also of each presentation form, something the author of these
 * routines has avoided to do so far.
 *
 * http://www.unicode.org/unicode/reports/tr11/
 *
 * Markus Kuhn -- 2007-05-25 (Unicode 5.0)
 *
 * Permission to use, copy, modify, and distribute this software
 * for any purpose and without fee is hereby granted. The author
 * disclaims all warranties with regard to this software.
 *
 * Latest version: http://www.cl.cam.ac.uk/~mgk25/ucs/wcwidth.c
 */

public class WCWidth {
	static int use_latin1 = 1;

	/*
	 * Provide a way to change the behavior of soft-hyphen.
	 */
	public static void mk_wcwidth_init(int mode)
	{
	  use_latin1 = mode == 0 ? 1 : 0;
	}
	
    private static class Interval {
        public final int first;
        public final int last;

        public Interval(int first, int last) {
            this.first = first;
            this.last = last;
        }
    }

    /* auxiliary function for binary search in interval table */
    private static boolean bisearch(int ucs, Interval[] table, int max) {
        int min = 0;
        int mid;

        if (ucs < table[0].first || ucs > table[max].last) return false;
        while (max >= min) {
            mid = (min + max) / 2;
            if (ucs > table[mid].last) min = mid + 1;
            else if (ucs < table[mid].first) max = mid - 1;
            else return true;
        }

        return false;
    }
    
    /* The following two functions define the column width of an ISO 10646
     * character as follows:
     *
     *    - The null character (U+0000) has a column width of 0.
     *
     *    - Other C0/C1 control characters and DEL will lead to a return
     *      value of -1.
     *
     *    - Non-spacing and enclosing combining characters (general
     *      category code Mn or Me in the Unicode database) have a
     *      column width of 0.
     *
     *    - A few spacing combining marks have a column width of 0.
     *
     *    - SOFT HYPHEN (U+00AD) has a column width of 1 in Latin-1, 0 in Unicode.
     *      An initialization function is used to switch between the two.
     *
     *    - Other format characters (general category code Cf in the Unicode
     *      database) and ZERO WIDTH SPACE (U+200B) have a column width of 0.
     *
     *    - Hangul Jamo medial vowels and final consonants (U+1160-U+11FF)
     *      have a column width of 0.
     *
     *    - Hangul Jamo Extended-B medial vowels and final consonants for old
     *      Korean (U+D7B0-U+D7FF) have a column width of 0.
     *
     *    - Spacing characters in the East Asian Wide (W) or East Asian
     *      Full-width (F) category as defined in Unicode Technical
     *      Report #11 have a column width of 2.  In that report, some codes
     *      were unassigned.  Characters in these blocks use a column width of 1:
     *          4DC0..4DFF; Yijing Hexagram Symbols
     *          A960..A97F; Hangul Jamo Extended-A
     *
     *    - All remaining characters (including all printable
     *      ISO 8859-1 and WGL4 characters, Unicode control characters,
     *      etc.) have a column width of 1.
     *
     *    - Codes which do not correspond to a Unicode character have a column
     *      width of -1.
     *
     * This implementation assumes that wchar_t characters are encoded
     * in ISO 10646.
     */

    /* sorted list of non-overlapping intervals of formatting characters */
    /* generated by
     *    uniset +cat=Cf -00AD -0600-0605 -061C -06DD -070F c
     */
    /* *INDENT-OFF* */
    /* generated by run-uniset_ctl 1.1 */
    static final Interval formatting[] = {
	    new Interval(0x0890, 0x0891), new Interval(0x08E2, 0x08E2 ), new Interval(0x180E, 0x180E ),
	    new Interval(0x200B, 0x200F ), new Interval(0x202A, 0x202E ), new Interval(0x2060, 0x2064 ),
	    new Interval(0x2066, 0x206F ), new Interval(0xFEFF, 0xFEFF ), new Interval(0xFFF9, 0xFFFB ),
		new Interval(0x110BD, 0x110BD ), new Interval(0x110CD, 0x110CD ), new Interval(0x13430, 0x1343F ),
		new Interval(0x1BCA0, 0x1BCA3 ), new Interval(0x1D173, 0x1D17A ), new Interval(0xE0001, 0xE0001 ),
		new Interval(0xE0020, 0xE007F )
    };    
    /* *INDENT-OFF* */
    
    /* sorted list of non-overlapping intervals of non-spacing characters */
    /* generated by
     *    uniset +cat=Me +cat=Mn +0600-0605 +061C +06DD +070F +1160-11FF +D7B0-D7C6 +D7CB-D7FB c 
     */
    /* *INDENT-OFF* */
    /* generated by run-uniset 1.9 */
    static final Interval[] combining = new Interval[] {
      new Interval( 0x0300, 0x036F ), new Interval( 0x0483, 0x0489 ), new Interval( 0x0591, 0x05BD ),
      new Interval( 0x05BF, 0x05BF ), new Interval( 0x05C1, 0x05C2 ), new Interval( 0x05C4, 0x05C5 ),
      new Interval( 0x05C7, 0x05C7 ), new Interval( 0x0600, 0x0605 ), new Interval( 0x0610, 0x061A ),
      new Interval( 0x061C, 0x061C ), new Interval( 0x064B, 0x065F ), new Interval( 0x0670, 0x0670 ),
      new Interval( 0x06D6, 0x06DD ), new Interval( 0x06DF, 0x06E4 ), new Interval( 0x06E7, 0x06E8 ),
      new Interval( 0x06EA, 0x06ED ), new Interval( 0x070F, 0x070F ), new Interval( 0x0711, 0x0711 ),
      new Interval( 0x0730, 0x074A ), new Interval( 0x07A6, 0x07B0 ), new Interval( 0x07EB, 0x07F3 ),
      new Interval( 0x07FD, 0x07FD ), new Interval( 0x0816, 0x0819 ), new Interval( 0x081B, 0x0823 ),
      new Interval( 0x0825, 0x0827 ), new Interval( 0x0829, 0x082D ), new Interval( 0x0859, 0x085B ),
      new Interval( 0x0898, 0x089F ), new Interval( 0x08CA, 0x08E1 ), new Interval( 0x08E3, 0x0902 ),
      new Interval( 0x093A, 0x093A ), new Interval( 0x093C, 0x093C ), new Interval( 0x0941, 0x0948 ),
      new Interval( 0x094D, 0x094D ), new Interval( 0x0951, 0x0957 ), new Interval( 0x0962, 0x0963 ),
      new Interval( 0x0981, 0x0981 ), new Interval( 0x09BC, 0x09BC ), new Interval( 0x09C1, 0x09C4 ),
      new Interval( 0x09CD, 0x09CD ), new Interval( 0x09E2, 0x09E3 ), new Interval( 0x09FE, 0x09FE ),
      new Interval( 0x0A01, 0x0A02 ), new Interval( 0x0A3C, 0x0A3C ), new Interval( 0x0A41, 0x0A42 ),
      new Interval( 0x0A47, 0x0A48 ), new Interval( 0x0A4B, 0x0A4D ), new Interval( 0x0A51, 0x0A51 ),
      new Interval( 0x0A70, 0x0A71 ), new Interval( 0x0A75, 0x0A75 ), new Interval( 0x0A81, 0x0A82 ),
      new Interval( 0x0ABC, 0x0ABC ), new Interval( 0x0AC1, 0x0AC5 ), new Interval( 0x0AC7, 0x0AC8 ),
      new Interval( 0x0ACD, 0x0ACD ), new Interval( 0x0AE2, 0x0AE3 ), new Interval( 0x0AFA, 0x0AFF ),
      new Interval( 0x0B01, 0x0B01 ), new Interval( 0x0B3C, 0x0B3C ), new Interval( 0x0B3F, 0x0B3F ),
      new Interval( 0x0B41, 0x0B44 ), new Interval( 0x0B4D, 0x0B4D ), new Interval( 0x0B55, 0x0B56 ),
      new Interval( 0x0B62, 0x0B63 ), new Interval( 0x0B82, 0x0B82 ), new Interval( 0x0BC0, 0x0BC0 ),
      new Interval( 0x0BCD, 0x0BCD ), new Interval( 0x0C00, 0x0C00 ), new Interval( 0x0C04, 0x0C04 ),
      new Interval( 0x0C3C, 0x0C3C ), new Interval( 0x0C3E, 0x0C40 ), new Interval( 0x0C46, 0x0C48 ),
      new Interval( 0x0C4A, 0x0C4D ), new Interval( 0x0C55, 0x0C56 ), new Interval( 0x0C62, 0x0C63 ),
      new Interval( 0x0C81, 0x0C81 ), new Interval( 0x0CBC, 0x0CBC ), new Interval( 0x0CBF, 0x0CBF ),
      new Interval( 0x0CC6, 0x0CC6 ), new Interval( 0x0CCC, 0x0CCD ), new Interval( 0x0CE2, 0x0CE3 ),
      new Interval( 0x0D00, 0x0D01 ), new Interval( 0x0D3B, 0x0D3C ), new Interval( 0x0D41, 0x0D44 ),
      new Interval( 0x0D4D, 0x0D4D ), new Interval( 0x0D62, 0x0D63 ), new Interval( 0x0D81, 0x0D81 ),
      new Interval( 0x0DCA, 0x0DCA ), new Interval( 0x0DD2, 0x0DD4 ), new Interval( 0x0DD6, 0x0DD6 ),
      new Interval( 0x0E31, 0x0E31 ), new Interval( 0x0E34, 0x0E3A ), new Interval( 0x0E47, 0x0E4E ),
      new Interval( 0x0EB1, 0x0EB1 ), new Interval( 0x0EB4, 0x0EBC ), new Interval( 0x0EC8, 0x0ECE ),
      new Interval( 0x0F18, 0x0F19 ), new Interval( 0x0F35, 0x0F35 ), new Interval( 0x0F37, 0x0F37 ),
      new Interval( 0x0F39, 0x0F39 ), new Interval( 0x0F71, 0x0F7E ), new Interval( 0x0F80, 0x0F84 ),
      new Interval( 0x0F86, 0x0F87 ), new Interval( 0x0F8D, 0x0F97 ), new Interval( 0x0F99, 0x0FBC ),
      new Interval( 0x0FC6, 0x0FC6 ), new Interval( 0x102D, 0x1030 ), new Interval( 0x1032, 0x1037 ),
      new Interval( 0x1039, 0x103A ), new Interval( 0x103D, 0x103E ), new Interval( 0x1058, 0x1059 ),
      new Interval( 0x105E, 0x1060 ), new Interval( 0x1071, 0x1074 ), new Interval( 0x1082, 0x1082 ),
      new Interval( 0x1085, 0x1086 ), new Interval( 0x108D, 0x108D ), new Interval( 0x109D, 0x109D ),
      new Interval( 0x1160, 0x11FF ), new Interval( 0x135D, 0x135F ), new Interval( 0x1712, 0x1714 ),
      new Interval( 0x1732, 0x1733 ), new Interval( 0x1752, 0x1753 ), new Interval( 0x1772, 0x1773 ),
      new Interval( 0x17B4, 0x17B5 ), new Interval( 0x17B7, 0x17BD ), new Interval( 0x17C6, 0x17C6 ),
      new Interval( 0x17C9, 0x17D3 ), new Interval( 0x17DD, 0x17DD ), new Interval( 0x180B, 0x180D ),
      new Interval( 0x180F, 0x180F ), new Interval( 0x1885, 0x1886 ), new Interval( 0x18A9, 0x18A9 ),
      new Interval( 0x1920, 0x1922 ), new Interval( 0x1927, 0x1928 ), new Interval( 0x1932, 0x1932 ),
      new Interval( 0x1939, 0x193B ), new Interval( 0x1A17, 0x1A18 ), new Interval( 0x1A1B, 0x1A1B ),
      new Interval( 0x1A56, 0x1A56 ), new Interval( 0x1A58, 0x1A5E ), new Interval( 0x1A60, 0x1A60 ),
      new Interval( 0x1A62, 0x1A62 ), new Interval( 0x1A65, 0x1A6C ), new Interval( 0x1A73, 0x1A7C ),
      new Interval( 0x1A7F, 0x1A7F ), new Interval( 0x1AB0, 0x1ACE ), new Interval( 0x1B00, 0x1B03 ),
      new Interval( 0x1B34, 0x1B34 ), new Interval( 0x1B36, 0x1B3A ), new Interval( 0x1B3C, 0x1B3C ),
      new Interval( 0x1B42, 0x1B42 ), new Interval( 0x1B6B, 0x1B73 ), new Interval( 0x1B80, 0x1B81 ),
      new Interval( 0x1BA2, 0x1BA5 ), new Interval( 0x1BA8, 0x1BA9 ), new Interval( 0x1BAB, 0x1BAD ),
      new Interval( 0x1BE6, 0x1BE6 ), new Interval( 0x1BE8, 0x1BE9 ), new Interval( 0x1BED, 0x1BED ),
      new Interval( 0x1BEF, 0x1BF1 ), new Interval( 0x1C2C, 0x1C33 ), new Interval( 0x1C36, 0x1C37 ),
      new Interval( 0x1CD0, 0x1CD2 ), new Interval( 0x1CD4, 0x1CE0 ), new Interval( 0x1CE2, 0x1CE8 ),
      new Interval( 0x1CED, 0x1CED ), new Interval( 0x1CF4, 0x1CF4 ), new Interval( 0x1CF8, 0x1CF9 ),
      new Interval( 0x1DC0, 0x1DFF ), new Interval( 0x20D0, 0x20F0 ), new Interval( 0x2CEF, 0x2CF1 ),
      new Interval( 0x2D7F, 0x2D7F ), new Interval( 0x2DE0, 0x2DFF ), new Interval( 0x302A, 0x302D ),
      new Interval( 0x3099, 0x309A ), new Interval( 0xA66F, 0xA672 ), new Interval( 0xA674, 0xA67D ),
      new Interval( 0xA69E, 0xA69F ), new Interval( 0xA6F0, 0xA6F1 ), new Interval( 0xA802, 0xA802 ),
      new Interval( 0xA806, 0xA806 ), new Interval( 0xA80B, 0xA80B ), new Interval( 0xA825, 0xA826 ),
      new Interval( 0xA82C, 0xA82C ), new Interval( 0xA8C4, 0xA8C5 ), new Interval( 0xA8E0, 0xA8F1 ),
      new Interval( 0xA8FF, 0xA8FF ), new Interval( 0xA926, 0xA92D ), new Interval( 0xA947, 0xA951 ),
      new Interval( 0xA980, 0xA982 ), new Interval( 0xA9B3, 0xA9B3 ), new Interval( 0xA9B6, 0xA9B9 ),
      new Interval( 0xA9BC, 0xA9BD ), new Interval( 0xA9E5, 0xA9E5 ), new Interval( 0xAA29, 0xAA2E ),
      new Interval( 0xAA31, 0xAA32 ), new Interval( 0xAA35, 0xAA36 ), new Interval( 0xAA43, 0xAA43 ),
      new Interval( 0xAA4C, 0xAA4C ), new Interval( 0xAA7C, 0xAA7C ), new Interval( 0xAAB0, 0xAAB0 ),
      new Interval( 0xAAB2, 0xAAB4 ), new Interval( 0xAAB7, 0xAAB8 ), new Interval( 0xAABE, 0xAABF ),
      new Interval( 0xAAC1, 0xAAC1 ), new Interval( 0xAAEC, 0xAAED ), new Interval( 0xAAF6, 0xAAF6 ),
      new Interval( 0xABE5, 0xABE5 ), new Interval( 0xABE8, 0xABE8 ), new Interval( 0xABED, 0xABED ),
      new Interval( 0xD7B0, 0xD7C6 ), new Interval( 0xD7CB, 0xD7FB ), new Interval( 0xFB1E, 0xFB1E ),
      new Interval( 0xFE00, 0xFE0F ), new Interval( 0xFE20, 0xFE2F ), new Interval( 0x101FD, 0x101FD ),
      new Interval( 0x102E0, 0x102E0 ), new Interval( 0x10376, 0x1037A ), new Interval( 0x10A01, 0x10A03 ),
      new Interval( 0x10A05, 0x10A06 ), new Interval( 0x10A0C, 0x10A0F ), new Interval( 0x10A38, 0x10A3A ),
      new Interval( 0x10A3F, 0x10A3F ), new Interval( 0x10AE5, 0x10AE6 ), new Interval( 0x10D24, 0x10D27 ),
      new Interval( 0x10EAB, 0x10EAC ), new Interval( 0x10EFD, 0x10EFF ), new Interval( 0x10F46, 0x10F50 ),
      new Interval( 0x10F82, 0x10F85 ), new Interval( 0x11001, 0x11001 ), new Interval( 0x11038, 0x11046 ),
      new Interval( 0x11070, 0x11070 ), new Interval( 0x11073, 0x11074 ), new Interval( 0x1107F, 0x11081 ),
      new Interval( 0x110B3, 0x110B6 ), new Interval( 0x110B9, 0x110BA ), new Interval( 0x110C2, 0x110C2 ),
      new Interval( 0x11100, 0x11102 ), new Interval( 0x11127, 0x1112B ), new Interval( 0x1112D, 0x11134 ),
      new Interval( 0x11173, 0x11173 ), new Interval( 0x11180, 0x11181 ), new Interval( 0x111B6, 0x111BE ),
      new Interval( 0x111C9, 0x111CC ), new Interval( 0x111CF, 0x111CF ), new Interval( 0x1122F, 0x11231 ),
      new Interval( 0x11234, 0x11234 ), new Interval( 0x11236, 0x11237 ), new Interval( 0x1123E, 0x1123E ),
      new Interval( 0x11241, 0x11241 ), new Interval( 0x112DF, 0x112DF ), new Interval( 0x112E3, 0x112EA ),
      new Interval( 0x11300, 0x11301 ), new Interval( 0x1133B, 0x1133C ), new Interval( 0x11340, 0x11340 ),
      new Interval( 0x11366, 0x1136C ), new Interval( 0x11370, 0x11374 ), new Interval( 0x11438, 0x1143F ),
      new Interval( 0x11442, 0x11444 ), new Interval( 0x11446, 0x11446 ), new Interval( 0x1145E, 0x1145E ),
      new Interval( 0x114B3, 0x114B8 ), new Interval( 0x114BA, 0x114BA ), new Interval( 0x114BF, 0x114C0 ),
      new Interval( 0x114C2, 0x114C3 ), new Interval( 0x115B2, 0x115B5 ), new Interval( 0x115BC, 0x115BD ),
      new Interval( 0x115BF, 0x115C0 ), new Interval( 0x115DC, 0x115DD ), new Interval( 0x11633, 0x1163A ),
      new Interval( 0x1163D, 0x1163D ), new Interval( 0x1163F, 0x11640 ), new Interval( 0x116AB, 0x116AB ),
      new Interval( 0x116AD, 0x116AD ), new Interval( 0x116B0, 0x116B5 ), new Interval( 0x116B7, 0x116B7 ),
      new Interval( 0x1171D, 0x1171F ), new Interval( 0x11722, 0x11725 ), new Interval( 0x11727, 0x1172B ),
      new Interval( 0x1182F, 0x11837 ), new Interval( 0x11839, 0x1183A ), new Interval( 0x1193B, 0x1193C ),
      new Interval( 0x1193E, 0x1193E ), new Interval( 0x11943, 0x11943 ), new Interval( 0x119D4, 0x119D7 ),
      new Interval( 0x119DA, 0x119DB ), new Interval( 0x119E0, 0x119E0 ), new Interval( 0x11A01, 0x11A0A ),
      new Interval( 0x11A33, 0x11A38 ), new Interval( 0x11A3B, 0x11A3E ), new Interval( 0x11A47, 0x11A47 ),
      new Interval( 0x11A51, 0x11A56 ), new Interval( 0x11A59, 0x11A5B ), new Interval( 0x11A8A, 0x11A96 ),
      new Interval( 0x11A98, 0x11A99 ), new Interval( 0x11C30, 0x11C36 ), new Interval( 0x11C38, 0x11C3D ),
      new Interval( 0x11C3F, 0x11C3F ), new Interval( 0x11C92, 0x11CA7 ), new Interval( 0x11CAA, 0x11CB0 ),
      new Interval( 0x11CB2, 0x11CB3 ), new Interval( 0x11CB5, 0x11CB6 ), new Interval( 0x11D31, 0x11D36 ),
      new Interval( 0x11D3A, 0x11D3A ), new Interval( 0x11D3C, 0x11D3D ), new Interval( 0x11D3F, 0x11D45 ),
      new Interval( 0x11D47, 0x11D47 ), new Interval( 0x11D90, 0x11D91 ), new Interval( 0x11D95, 0x11D95 ),
      new Interval( 0x11D97, 0x11D97 ), new Interval( 0x11EF3, 0x11EF4 ), new Interval( 0x11F00, 0x11F01 ),
      new Interval( 0x11F36, 0x11F3A ), new Interval( 0x11F40, 0x11F40 ), new Interval( 0x11F42, 0x11F42 ),
      new Interval( 0x13440, 0x13440 ), new Interval( 0x13447, 0x13455 ), new Interval( 0x16AF0, 0x16AF4 ),
      new Interval( 0x16B30, 0x16B36 ), new Interval( 0x16F4F, 0x16F4F ), new Interval( 0x16F8F, 0x16F92 ),
      new Interval( 0x16FE4, 0x16FE4 ), new Interval( 0x1BC9D, 0x1BC9E ), new Interval( 0x1CF00, 0x1CF2D ),
      new Interval( 0x1CF30, 0x1CF46 ), new Interval( 0x1D167, 0x1D169 ), new Interval( 0x1D17B, 0x1D182 ),
      new Interval( 0x1D185, 0x1D18B ), new Interval( 0x1D1AA, 0x1D1AD ), new Interval( 0x1D242, 0x1D244 ),
      new Interval( 0x1DA00, 0x1DA36 ), new Interval( 0x1DA3B, 0x1DA6C ), new Interval( 0x1DA75, 0x1DA75 ),
      new Interval( 0x1DA84, 0x1DA84 ), new Interval( 0x1DA9B, 0x1DA9F ), new Interval( 0x1DAA1, 0x1DAAF ),
      new Interval( 0x1E000, 0x1E006 ), new Interval( 0x1E008, 0x1E018 ), new Interval( 0x1E01B, 0x1E021 ),
      new Interval( 0x1E023, 0x1E024 ), new Interval( 0x1E026, 0x1E02A ), new Interval( 0x1E08F, 0x1E08F ),
      new Interval( 0x1E130, 0x1E136 ), new Interval( 0x1E2AE, 0x1E2AE ), new Interval( 0x1E2EC, 0x1E2EF ),
      new Interval( 0x1E4EC, 0x1E4EF ), new Interval( 0x1E8D0, 0x1E8D6 ), new Interval( 0x1E944, 0x1E94A ),
      new Interval( 0xE0100, 0xE01EF )
    };
    /* *INDENT-ON* */
    
    /* sorted list of non-overlapping intervals of non-characters */
    /* generated by
     *    uniset +0000..DFFF -4e00..9fd5 +F900..10FFFD unknown +2028..2029 c
     */
    /* *INDENT-OFF* */
    /* generated by run-uniset_unk 1.6 */
    static final Interval[] unknowns = new Interval[] {
      new Interval( 0x0378, 0x0379 ), new Interval( 0x0380, 0x0383 ), new Interval( 0x038B, 0x038B ),
      new Interval( 0x038D, 0x038D ), new Interval( 0x03A2, 0x03A2 ), new Interval( 0x0530, 0x0530 ),
      new Interval( 0x0557, 0x0558 ), new Interval( 0x058B, 0x058C ), new Interval( 0x0590, 0x0590 ),
      new Interval( 0x05C8, 0x05CF ), new Interval( 0x05EB, 0x05EE ), new Interval( 0x05F5, 0x05FF ),
      new Interval( 0x070E, 0x070E ), new Interval( 0x074B, 0x074C ), new Interval( 0x07B2, 0x07BF ),
      new Interval( 0x07FB, 0x07FC ), new Interval( 0x082E, 0x082F ), new Interval( 0x083F, 0x083F ),
      new Interval( 0x085C, 0x085D ), new Interval( 0x085F, 0x085F ), new Interval( 0x086B, 0x086F ),
      new Interval( 0x088F, 0x088F ), new Interval( 0x0892, 0x0897 ), new Interval( 0x0984, 0x0984 ),
      new Interval( 0x098D, 0x098E ), new Interval( 0x0991, 0x0992 ), new Interval( 0x09A9, 0x09A9 ),
      new Interval( 0x09B1, 0x09B1 ), new Interval( 0x09B3, 0x09B5 ), new Interval( 0x09BA, 0x09BB ),
      new Interval( 0x09C5, 0x09C6 ), new Interval( 0x09C9, 0x09CA ), new Interval( 0x09CF, 0x09D6 ),
      new Interval( 0x09D8, 0x09DB ), new Interval( 0x09DE, 0x09DE ), new Interval( 0x09E4, 0x09E5 ),
      new Interval( 0x09FF, 0x0A00 ), new Interval( 0x0A04, 0x0A04 ), new Interval( 0x0A0B, 0x0A0E ),
      new Interval( 0x0A11, 0x0A12 ), new Interval( 0x0A29, 0x0A29 ), new Interval( 0x0A31, 0x0A31 ),
      new Interval( 0x0A34, 0x0A34 ), new Interval( 0x0A37, 0x0A37 ), new Interval( 0x0A3A, 0x0A3B ),
      new Interval( 0x0A3D, 0x0A3D ), new Interval( 0x0A43, 0x0A46 ), new Interval( 0x0A49, 0x0A4A ),
      new Interval( 0x0A4E, 0x0A50 ), new Interval( 0x0A52, 0x0A58 ), new Interval( 0x0A5D, 0x0A5D ),
      new Interval( 0x0A5F, 0x0A65 ), new Interval( 0x0A77, 0x0A80 ), new Interval( 0x0A84, 0x0A84 ),
      new Interval( 0x0A8E, 0x0A8E ), new Interval( 0x0A92, 0x0A92 ), new Interval( 0x0AA9, 0x0AA9 ),
      new Interval( 0x0AB1, 0x0AB1 ), new Interval( 0x0AB4, 0x0AB4 ), new Interval( 0x0ABA, 0x0ABB ),
      new Interval( 0x0AC6, 0x0AC6 ), new Interval( 0x0ACA, 0x0ACA ), new Interval( 0x0ACE, 0x0ACF ),
      new Interval( 0x0AD1, 0x0ADF ), new Interval( 0x0AE4, 0x0AE5 ), new Interval( 0x0AF2, 0x0AF8 ),
      new Interval( 0x0B00, 0x0B00 ), new Interval( 0x0B04, 0x0B04 ), new Interval( 0x0B0D, 0x0B0E ),
      new Interval( 0x0B11, 0x0B12 ), new Interval( 0x0B29, 0x0B29 ), new Interval( 0x0B31, 0x0B31 ),
      new Interval( 0x0B34, 0x0B34 ), new Interval( 0x0B3A, 0x0B3B ), new Interval( 0x0B45, 0x0B46 ),
      new Interval( 0x0B49, 0x0B4A ), new Interval( 0x0B4E, 0x0B54 ), new Interval( 0x0B58, 0x0B5B ),
      new Interval( 0x0B5E, 0x0B5E ), new Interval( 0x0B64, 0x0B65 ), new Interval( 0x0B78, 0x0B81 ),
      new Interval( 0x0B84, 0x0B84 ), new Interval( 0x0B8B, 0x0B8D ), new Interval( 0x0B91, 0x0B91 ),
      new Interval( 0x0B96, 0x0B98 ), new Interval( 0x0B9B, 0x0B9B ), new Interval( 0x0B9D, 0x0B9D ),
      new Interval( 0x0BA0, 0x0BA2 ), new Interval( 0x0BA5, 0x0BA7 ), new Interval( 0x0BAB, 0x0BAD ),
      new Interval( 0x0BBA, 0x0BBD ), new Interval( 0x0BC3, 0x0BC5 ), new Interval( 0x0BC9, 0x0BC9 ),
      new Interval( 0x0BCE, 0x0BCF ), new Interval( 0x0BD1, 0x0BD6 ), new Interval( 0x0BD8, 0x0BE5 ),
      new Interval( 0x0BFB, 0x0BFF ), new Interval( 0x0C0D, 0x0C0D ), new Interval( 0x0C11, 0x0C11 ),
      new Interval( 0x0C29, 0x0C29 ), new Interval( 0x0C3A, 0x0C3B ), new Interval( 0x0C45, 0x0C45 ),
      new Interval( 0x0C49, 0x0C49 ), new Interval( 0x0C4E, 0x0C54 ), new Interval( 0x0C57, 0x0C57 ),
      new Interval( 0x0C5B, 0x0C5C ), new Interval( 0x0C5E, 0x0C5F ), new Interval( 0x0C64, 0x0C65 ),
      new Interval( 0x0C70, 0x0C76 ), new Interval( 0x0C8D, 0x0C8D ), new Interval( 0x0C91, 0x0C91 ),
      new Interval( 0x0CA9, 0x0CA9 ), new Interval( 0x0CB4, 0x0CB4 ), new Interval( 0x0CBA, 0x0CBB ),
      new Interval( 0x0CC5, 0x0CC5 ), new Interval( 0x0CC9, 0x0CC9 ), new Interval( 0x0CCE, 0x0CD4 ),
      new Interval( 0x0CD7, 0x0CDC ), new Interval( 0x0CDF, 0x0CDF ), new Interval( 0x0CE4, 0x0CE5 ),
      new Interval( 0x0CF0, 0x0CF0 ), new Interval( 0x0CF4, 0x0CFF ), new Interval( 0x0D0D, 0x0D0D ),
      new Interval( 0x0D11, 0x0D11 ), new Interval( 0x0D45, 0x0D45 ), new Interval( 0x0D49, 0x0D49 ),
      new Interval( 0x0D50, 0x0D53 ), new Interval( 0x0D64, 0x0D65 ), new Interval( 0x0D80, 0x0D80 ),
      new Interval( 0x0D84, 0x0D84 ), new Interval( 0x0D97, 0x0D99 ), new Interval( 0x0DB2, 0x0DB2 ),
      new Interval( 0x0DBC, 0x0DBC ), new Interval( 0x0DBE, 0x0DBF ), new Interval( 0x0DC7, 0x0DC9 ),
      new Interval( 0x0DCB, 0x0DCE ), new Interval( 0x0DD5, 0x0DD5 ), new Interval( 0x0DD7, 0x0DD7 ),
      new Interval( 0x0DE0, 0x0DE5 ), new Interval( 0x0DF0, 0x0DF1 ), new Interval( 0x0DF5, 0x0E00 ),
      new Interval( 0x0E3B, 0x0E3E ), new Interval( 0x0E5C, 0x0E80 ), new Interval( 0x0E83, 0x0E83 ),
      new Interval( 0x0E85, 0x0E85 ), new Interval( 0x0E8B, 0x0E8B ), new Interval( 0x0EA4, 0x0EA4 ),
      new Interval( 0x0EA6, 0x0EA6 ), new Interval( 0x0EBE, 0x0EBF ), new Interval( 0x0EC5, 0x0EC5 ),
      new Interval( 0x0EC7, 0x0EC7 ), new Interval( 0x0ECF, 0x0ECF ), new Interval( 0x0EDA, 0x0EDB ),
      new Interval( 0x0EE0, 0x0EFF ), new Interval( 0x0F48, 0x0F48 ), new Interval( 0x0F6D, 0x0F70 ),
      new Interval( 0x0F98, 0x0F98 ), new Interval( 0x0FBD, 0x0FBD ), new Interval( 0x0FCD, 0x0FCD ),
      new Interval( 0x0FDB, 0x0FFF ), new Interval( 0x10C6, 0x10C6 ), new Interval( 0x10C8, 0x10CC ),
      new Interval( 0x10CE, 0x10CF ), new Interval( 0x1249, 0x1249 ), new Interval( 0x124E, 0x124F ),
      new Interval( 0x1257, 0x1257 ), new Interval( 0x1259, 0x1259 ), new Interval( 0x125E, 0x125F ),
      new Interval( 0x1289, 0x1289 ), new Interval( 0x128E, 0x128F ), new Interval( 0x12B1, 0x12B1 ),
      new Interval( 0x12B6, 0x12B7 ), new Interval( 0x12BF, 0x12BF ), new Interval( 0x12C1, 0x12C1 ),
      new Interval( 0x12C6, 0x12C7 ), new Interval( 0x12D7, 0x12D7 ), new Interval( 0x1311, 0x1311 ),
      new Interval( 0x1316, 0x1317 ), new Interval( 0x135B, 0x135C ), new Interval( 0x137D, 0x137F ),
      new Interval( 0x139A, 0x139F ), new Interval( 0x13F6, 0x13F7 ), new Interval( 0x13FE, 0x13FF ),
      new Interval( 0x169D, 0x169F ), new Interval( 0x16F9, 0x16FF ), new Interval( 0x1716, 0x171E ),
      new Interval( 0x1737, 0x173F ), new Interval( 0x1754, 0x175F ), new Interval( 0x176D, 0x176D ),
      new Interval( 0x1771, 0x1771 ), new Interval( 0x1774, 0x177F ), new Interval( 0x17DE, 0x17DF ),
      new Interval( 0x17EA, 0x17EF ), new Interval( 0x17FA, 0x17FF ), new Interval( 0x181A, 0x181F ),
      new Interval( 0x1879, 0x187F ), new Interval( 0x18AB, 0x18AF ), new Interval( 0x18F6, 0x18FF ),
      new Interval( 0x191F, 0x191F ), new Interval( 0x192C, 0x192F ), new Interval( 0x193C, 0x193F ),
      new Interval( 0x1941, 0x1943 ), new Interval( 0x196E, 0x196F ), new Interval( 0x1975, 0x197F ),
      new Interval( 0x19AC, 0x19AF ), new Interval( 0x19CA, 0x19CF ), new Interval( 0x19DB, 0x19DD ),
      new Interval( 0x1A1C, 0x1A1D ), new Interval( 0x1A5F, 0x1A5F ), new Interval( 0x1A7D, 0x1A7E ),
      new Interval( 0x1A8A, 0x1A8F ), new Interval( 0x1A9A, 0x1A9F ), new Interval( 0x1AAE, 0x1AAF ),
      new Interval( 0x1ACF, 0x1AFF ), new Interval( 0x1B4D, 0x1B4F ), new Interval( 0x1B7F, 0x1B7F ),
      new Interval( 0x1BF4, 0x1BFB ), new Interval( 0x1C38, 0x1C3A ), new Interval( 0x1C4A, 0x1C4C ),
      new Interval( 0x1C89, 0x1C8F ), new Interval( 0x1CBB, 0x1CBC ), new Interval( 0x1CC8, 0x1CCF ),
      new Interval( 0x1CFB, 0x1CFF ), new Interval( 0x1F16, 0x1F17 ), new Interval( 0x1F1E, 0x1F1F ),
      new Interval( 0x1F46, 0x1F47 ), new Interval( 0x1F4E, 0x1F4F ), new Interval( 0x1F58, 0x1F58 ),
      new Interval( 0x1F5A, 0x1F5A ), new Interval( 0x1F5C, 0x1F5C ), new Interval( 0x1F5E, 0x1F5E ),
      new Interval( 0x1F7E, 0x1F7F ), new Interval( 0x1FB5, 0x1FB5 ), new Interval( 0x1FC5, 0x1FC5 ),
      new Interval( 0x1FD4, 0x1FD5 ), new Interval( 0x1FDC, 0x1FDC ), new Interval( 0x1FF0, 0x1FF1 ),
      new Interval( 0x1FF5, 0x1FF5 ), new Interval( 0x1FFF, 0x1FFF ), new Interval( 0x2028, 0x2029 ),
      new Interval( 0x2065, 0x2065 ), new Interval( 0x2072, 0x2073 ), new Interval( 0x208F, 0x208F ),
      new Interval( 0x209D, 0x209F ), new Interval( 0x20C1, 0x20CF ), new Interval( 0x20F1, 0x20FF ),
      new Interval( 0x218C, 0x218F ), new Interval( 0x2427, 0x243F ), new Interval( 0x244B, 0x245F ),
      new Interval( 0x2B74, 0x2B75 ), new Interval( 0x2B96, 0x2B96 ), new Interval( 0x2CF4, 0x2CF8 ),
      new Interval( 0x2D26, 0x2D26 ), new Interval( 0x2D28, 0x2D2C ), new Interval( 0x2D2E, 0x2D2F ),
      new Interval( 0x2D68, 0x2D6E ), new Interval( 0x2D71, 0x2D7E ), new Interval( 0x2D97, 0x2D9F ),
      new Interval( 0x2DA7, 0x2DA7 ), new Interval( 0x2DAF, 0x2DAF ), new Interval( 0x2DB7, 0x2DB7 ),
      new Interval( 0x2DBF, 0x2DBF ), new Interval( 0x2DC7, 0x2DC7 ), new Interval( 0x2DCF, 0x2DCF ),
      new Interval( 0x2DD7, 0x2DD7 ), new Interval( 0x2DDF, 0x2DDF ), new Interval( 0x2E5E, 0x2E7F ),
      new Interval( 0x2E9A, 0x2E9A ), new Interval( 0x2EF4, 0x2EFF ), new Interval( 0x2FD6, 0x2FEF ),
      new Interval( 0x3040, 0x3040 ), new Interval( 0x3097, 0x3098 ), new Interval( 0x3100, 0x3104 ),
      new Interval( 0x3130, 0x3130 ), new Interval( 0x318F, 0x318F ), new Interval( 0x31E4, 0x31EE ),
      new Interval( 0x321F, 0x321F ), new Interval( 0x4DB6, 0x4DBF ), new Interval( 0x9FD6, 0x9FFF ),
      new Interval( 0xA48D, 0xA48F ), new Interval( 0xA4C7, 0xA4CF ), new Interval( 0xA62C, 0xA63F ),
      new Interval( 0xA6F8, 0xA6FF ), new Interval( 0xA7CB, 0xA7CF ), new Interval( 0xA7D2, 0xA7D2 ),
      new Interval( 0xA7D4, 0xA7D4 ), new Interval( 0xA7DA, 0xA7F1 ), new Interval( 0xA82D, 0xA82F ),
      new Interval( 0xA83A, 0xA83F ), new Interval( 0xA878, 0xA87F ), new Interval( 0xA8C6, 0xA8CD ),
      new Interval( 0xA8DA, 0xA8DF ), new Interval( 0xA954, 0xA95E ), new Interval( 0xA97D, 0xA97F ),
      new Interval( 0xA9CE, 0xA9CE ), new Interval( 0xA9DA, 0xA9DD ), new Interval( 0xA9FF, 0xA9FF ),
      new Interval( 0xAA37, 0xAA3F ), new Interval( 0xAA4E, 0xAA4F ), new Interval( 0xAA5A, 0xAA5B ),
      new Interval( 0xAAC3, 0xAADA ), new Interval( 0xAAF7, 0xAB00 ), new Interval( 0xAB07, 0xAB08 ),
      new Interval( 0xAB0F, 0xAB10 ), new Interval( 0xAB17, 0xAB1F ), new Interval( 0xAB27, 0xAB27 ),
      new Interval( 0xAB2F, 0xAB2F ), new Interval( 0xAB6C, 0xAB6F ), new Interval( 0xABEE, 0xABEF ),
      new Interval( 0xABFA, 0xABFF ), new Interval( 0xD7A4, 0xD7AF ), new Interval( 0xD7C7, 0xD7CA ),
      new Interval( 0xD7FC, 0xDFFF ), new Interval( 0xFA6E, 0xFA6F ), new Interval( 0xFADA, 0xFAFF ),
      new Interval( 0xFB07, 0xFB12 ), new Interval( 0xFB18, 0xFB1C ), new Interval( 0xFB37, 0xFB37 ),
      new Interval( 0xFB3D, 0xFB3D ), new Interval( 0xFB3F, 0xFB3F ), new Interval( 0xFB42, 0xFB42 ),
      new Interval( 0xFB45, 0xFB45 ), new Interval( 0xFBC3, 0xFBD2 ), new Interval( 0xFD90, 0xFD91 ),
      new Interval( 0xFDC8, 0xFDCE ), new Interval( 0xFDD0, 0xFDEF ), new Interval( 0xFE1A, 0xFE1F ),
      new Interval( 0xFE53, 0xFE53 ), new Interval( 0xFE67, 0xFE67 ), new Interval( 0xFE6C, 0xFE6F ),
      new Interval( 0xFE75, 0xFE75 ), new Interval( 0xFEFD, 0xFEFE ), new Interval( 0xFF00, 0xFF00 ),
      new Interval( 0xFFBF, 0xFFC1 ), new Interval( 0xFFC8, 0xFFC9 ), new Interval( 0xFFD0, 0xFFD1 ),
      new Interval( 0xFFD8, 0xFFD9 ), new Interval( 0xFFDD, 0xFFDF ), new Interval( 0xFFE7, 0xFFE7 ),
      new Interval( 0xFFEF, 0xFFF8 ), new Interval( 0xFFFE, 0xFFFF ), new Interval( 0x1000C, 0x1000C ),
      new Interval( 0x10027, 0x10027 ), new Interval( 0x1003B, 0x1003B ), new Interval( 0x1003E, 0x1003E ),
      new Interval( 0x1004E, 0x1004F ), new Interval( 0x1005E, 0x1007F ), new Interval( 0x100FB, 0x100FF ),
      new Interval( 0x10103, 0x10106 ), new Interval( 0x10134, 0x10136 ), new Interval( 0x1018F, 0x1018F ),
      new Interval( 0x1019D, 0x1019F ), new Interval( 0x101A1, 0x101CF ), new Interval( 0x101FE, 0x1027F ),
      new Interval( 0x1029D, 0x1029F ), new Interval( 0x102D1, 0x102DF ), new Interval( 0x102FC, 0x102FF ),
      new Interval( 0x10324, 0x1032C ), new Interval( 0x1034B, 0x1034F ), new Interval( 0x1037B, 0x1037F ),
      new Interval( 0x1039E, 0x1039E ), new Interval( 0x103C4, 0x103C7 ), new Interval( 0x103D6, 0x103FF ),
      new Interval( 0x1049E, 0x1049F ), new Interval( 0x104AA, 0x104AF ), new Interval( 0x104D4, 0x104D7 ),
      new Interval( 0x104FC, 0x104FF ), new Interval( 0x10528, 0x1052F ), new Interval( 0x10564, 0x1056E ),
      new Interval( 0x1057B, 0x1057B ), new Interval( 0x1058B, 0x1058B ), new Interval( 0x10593, 0x10593 ),
      new Interval( 0x10596, 0x10596 ), new Interval( 0x105A2, 0x105A2 ), new Interval( 0x105B2, 0x105B2 ),
      new Interval( 0x105BA, 0x105BA ), new Interval( 0x105BD, 0x105FF ), new Interval( 0x10737, 0x1073F ),
      new Interval( 0x10756, 0x1075F ), new Interval( 0x10768, 0x1077F ), new Interval( 0x10786, 0x10786 ),
      new Interval( 0x107B1, 0x107B1 ), new Interval( 0x107BB, 0x107FF ), new Interval( 0x10806, 0x10807 ),
      new Interval( 0x10809, 0x10809 ), new Interval( 0x10836, 0x10836 ), new Interval( 0x10839, 0x1083B ),
      new Interval( 0x1083D, 0x1083E ), new Interval( 0x10856, 0x10856 ), new Interval( 0x1089F, 0x108A6 ),
      new Interval( 0x108B0, 0x108DF ), new Interval( 0x108F3, 0x108F3 ), new Interval( 0x108F6, 0x108FA ),
      new Interval( 0x1091C, 0x1091E ), new Interval( 0x1093A, 0x1093E ), new Interval( 0x10940, 0x1097F ),
      new Interval( 0x109B8, 0x109BB ), new Interval( 0x109D0, 0x109D1 ), new Interval( 0x10A04, 0x10A04 ),
      new Interval( 0x10A07, 0x10A0B ), new Interval( 0x10A14, 0x10A14 ), new Interval( 0x10A18, 0x10A18 ),
      new Interval( 0x10A36, 0x10A37 ), new Interval( 0x10A3B, 0x10A3E ), new Interval( 0x10A49, 0x10A4F ),
      new Interval( 0x10A59, 0x10A5F ), new Interval( 0x10AA0, 0x10ABF ), new Interval( 0x10AE7, 0x10AEA ),
      new Interval( 0x10AF7, 0x10AFF ), new Interval( 0x10B36, 0x10B38 ), new Interval( 0x10B56, 0x10B57 ),
      new Interval( 0x10B73, 0x10B77 ), new Interval( 0x10B92, 0x10B98 ), new Interval( 0x10B9D, 0x10BA8 ),
      new Interval( 0x10BB0, 0x10BFF ), new Interval( 0x10C49, 0x10C7F ), new Interval( 0x10CB3, 0x10CBF ),
      new Interval( 0x10CF3, 0x10CF9 ), new Interval( 0x10D28, 0x10D2F ), new Interval( 0x10D3A, 0x10E5F ),
      new Interval( 0x10E7F, 0x10E7F ), new Interval( 0x10EAA, 0x10EAA ), new Interval( 0x10EAE, 0x10EAF ),
      new Interval( 0x10EB2, 0x10EFC ), new Interval( 0x10F28, 0x10F2F ), new Interval( 0x10F5A, 0x10F6F ),
      new Interval( 0x10F8A, 0x10FAF ), new Interval( 0x10FCC, 0x10FDF ), new Interval( 0x10FF7, 0x10FFF ),
      new Interval( 0x1104E, 0x11051 ), new Interval( 0x11076, 0x1107E ), new Interval( 0x110C3, 0x110CC ),
      new Interval( 0x110CE, 0x110CF ), new Interval( 0x110E9, 0x110EF ), new Interval( 0x110FA, 0x110FF ),
      new Interval( 0x11135, 0x11135 ), new Interval( 0x11148, 0x1114F ), new Interval( 0x11177, 0x1117F ),
      new Interval( 0x111E0, 0x111E0 ), new Interval( 0x111F5, 0x111FF ), new Interval( 0x11212, 0x11212 ),
      new Interval( 0x11242, 0x1127F ), new Interval( 0x11287, 0x11287 ), new Interval( 0x11289, 0x11289 ),
      new Interval( 0x1128E, 0x1128E ), new Interval( 0x1129E, 0x1129E ), new Interval( 0x112AA, 0x112AF ),
      new Interval( 0x112EB, 0x112EF ), new Interval( 0x112FA, 0x112FF ), new Interval( 0x11304, 0x11304 ),
      new Interval( 0x1130D, 0x1130E ), new Interval( 0x11311, 0x11312 ), new Interval( 0x11329, 0x11329 ),
      new Interval( 0x11331, 0x11331 ), new Interval( 0x11334, 0x11334 ), new Interval( 0x1133A, 0x1133A ),
      new Interval( 0x11345, 0x11346 ), new Interval( 0x11349, 0x1134A ), new Interval( 0x1134E, 0x1134F ),
      new Interval( 0x11351, 0x11356 ), new Interval( 0x11358, 0x1135C ), new Interval( 0x11364, 0x11365 ),
      new Interval( 0x1136D, 0x1136F ), new Interval( 0x11375, 0x113FF ), new Interval( 0x1145C, 0x1145C ),
      new Interval( 0x11462, 0x1147F ), new Interval( 0x114C8, 0x114CF ), new Interval( 0x114DA, 0x1157F ),
      new Interval( 0x115B6, 0x115B7 ), new Interval( 0x115DE, 0x115FF ), new Interval( 0x11645, 0x1164F ),
      new Interval( 0x1165A, 0x1165F ), new Interval( 0x1166D, 0x1167F ), new Interval( 0x116BA, 0x116BF ),
      new Interval( 0x116CA, 0x116FF ), new Interval( 0x1171B, 0x1171C ), new Interval( 0x1172C, 0x1172F ),
      new Interval( 0x11747, 0x117FF ), new Interval( 0x1183C, 0x1189F ), new Interval( 0x118F3, 0x118FE ),
      new Interval( 0x11907, 0x11908 ), new Interval( 0x1190A, 0x1190B ), new Interval( 0x11914, 0x11914 ),
      new Interval( 0x11917, 0x11917 ), new Interval( 0x11936, 0x11936 ), new Interval( 0x11939, 0x1193A ),
      new Interval( 0x11947, 0x1194F ), new Interval( 0x1195A, 0x1199F ), new Interval( 0x119A8, 0x119A9 ),
      new Interval( 0x119D8, 0x119D9 ), new Interval( 0x119E5, 0x119FF ), new Interval( 0x11A48, 0x11A4F ),
      new Interval( 0x11AA3, 0x11AAF ), new Interval( 0x11AF9, 0x11AFF ), new Interval( 0x11B0A, 0x11BFF ),
      new Interval( 0x11C09, 0x11C09 ), new Interval( 0x11C37, 0x11C37 ), new Interval( 0x11C46, 0x11C4F ),
      new Interval( 0x11C6D, 0x11C6F ), new Interval( 0x11C90, 0x11C91 ), new Interval( 0x11CA8, 0x11CA8 ),
      new Interval( 0x11CB7, 0x11CFF ), new Interval( 0x11D07, 0x11D07 ), new Interval( 0x11D0A, 0x11D0A ),
      new Interval( 0x11D37, 0x11D39 ), new Interval( 0x11D3B, 0x11D3B ), new Interval( 0x11D3E, 0x11D3E ),
      new Interval( 0x11D48, 0x11D4F ), new Interval( 0x11D5A, 0x11D5F ), new Interval( 0x11D66, 0x11D66 ),
      new Interval( 0x11D69, 0x11D69 ), new Interval( 0x11D8F, 0x11D8F ), new Interval( 0x11D92, 0x11D92 ),
      new Interval( 0x11D99, 0x11D9F ), new Interval( 0x11DAA, 0x11EDF ), new Interval( 0x11EF9, 0x11EFF ),
      new Interval( 0x11F11, 0x11F11 ), new Interval( 0x11F3B, 0x11F3D ), new Interval( 0x11F5A, 0x11FAF ),
      new Interval( 0x11FB1, 0x11FBF ), new Interval( 0x11FF2, 0x11FFE ), new Interval( 0x1239A, 0x123FF ),
      new Interval( 0x1246F, 0x1246F ), new Interval( 0x12475, 0x1247F ), new Interval( 0x12544, 0x12F8F ),
      new Interval( 0x12FF3, 0x12FFF ), new Interval( 0x13456, 0x143FF ), new Interval( 0x14647, 0x167FF ),
      new Interval( 0x16A39, 0x16A3F ), new Interval( 0x16A5F, 0x16A5F ), new Interval( 0x16A6A, 0x16A6D ),
      new Interval( 0x16ABF, 0x16ABF ), new Interval( 0x16ACA, 0x16ACF ), new Interval( 0x16AEE, 0x16AEF ),
      new Interval( 0x16AF6, 0x16AFF ), new Interval( 0x16B46, 0x16B4F ), new Interval( 0x16B5A, 0x16B5A ),
      new Interval( 0x16B62, 0x16B62 ), new Interval( 0x16B78, 0x16B7C ), new Interval( 0x16B90, 0x16E3F ),
      new Interval( 0x16E9B, 0x16EFF ), new Interval( 0x16F4B, 0x16F4E ), new Interval( 0x16F88, 0x16F8E ),
      new Interval( 0x16FA0, 0x16FDF ), new Interval( 0x16FE5, 0x16FEF ), new Interval( 0x16FF2, 0x187FF ),
      new Interval( 0x18CD6, 0x1AFEF ), new Interval( 0x1AFF4, 0x1AFF4 ), new Interval( 0x1AFFC, 0x1AFFC ),
      new Interval( 0x1AFFF, 0x1AFFF ), new Interval( 0x1B123, 0x1B131 ), new Interval( 0x1B133, 0x1B14F ),
      new Interval( 0x1B153, 0x1B154 ), new Interval( 0x1B156, 0x1B163 ), new Interval( 0x1B168, 0x1B16F ),
      new Interval( 0x1B2FC, 0x1BBFF ), new Interval( 0x1BC6B, 0x1BC6F ), new Interval( 0x1BC7D, 0x1BC7F ),
      new Interval( 0x1BC89, 0x1BC8F ), new Interval( 0x1BC9A, 0x1BC9B ), new Interval( 0x1BCA4, 0x1CEFF ),
      new Interval( 0x1CF2E, 0x1CF2F ), new Interval( 0x1CF47, 0x1CF4F ), new Interval( 0x1CFC4, 0x1CFFF ),
      new Interval( 0x1D0F6, 0x1D0FF ), new Interval( 0x1D127, 0x1D128 ), new Interval( 0x1D1EB, 0x1D1FF ),
      new Interval( 0x1D246, 0x1D2BF ), new Interval( 0x1D2D4, 0x1D2DF ), new Interval( 0x1D2F4, 0x1D2FF ),
      new Interval( 0x1D357, 0x1D35F ), new Interval( 0x1D379, 0x1D3FF ), new Interval( 0x1D455, 0x1D455 ),
      new Interval( 0x1D49D, 0x1D49D ), new Interval( 0x1D4A0, 0x1D4A1 ), new Interval( 0x1D4A3, 0x1D4A4 ),
      new Interval( 0x1D4A7, 0x1D4A8 ), new Interval( 0x1D4AD, 0x1D4AD ), new Interval( 0x1D4BA, 0x1D4BA ),
      new Interval( 0x1D4BC, 0x1D4BC ), new Interval( 0x1D4C4, 0x1D4C4 ), new Interval( 0x1D506, 0x1D506 ),
      new Interval( 0x1D50B, 0x1D50C ), new Interval( 0x1D515, 0x1D515 ), new Interval( 0x1D51D, 0x1D51D ),
      new Interval( 0x1D53A, 0x1D53A ), new Interval( 0x1D53F, 0x1D53F ), new Interval( 0x1D545, 0x1D545 ),
      new Interval( 0x1D547, 0x1D549 ), new Interval( 0x1D551, 0x1D551 ), new Interval( 0x1D6A6, 0x1D6A7 ),
      new Interval( 0x1D7CC, 0x1D7CD ), new Interval( 0x1DA8C, 0x1DA9A ), new Interval( 0x1DAA0, 0x1DAA0 ),
      new Interval( 0x1DAB0, 0x1DEFF ), new Interval( 0x1DF1F, 0x1DF24 ), new Interval( 0x1DF2B, 0x1DFFF ),
      new Interval( 0x1E007, 0x1E007 ), new Interval( 0x1E019, 0x1E01A ), new Interval( 0x1E022, 0x1E022 ),
      new Interval( 0x1E025, 0x1E025 ), new Interval( 0x1E02B, 0x1E02F ), new Interval( 0x1E06E, 0x1E08E ),
      new Interval( 0x1E090, 0x1E0FF ), new Interval( 0x1E12D, 0x1E12F ), new Interval( 0x1E13E, 0x1E13F ),
      new Interval( 0x1E14A, 0x1E14D ), new Interval( 0x1E150, 0x1E28F ), new Interval( 0x1E2AF, 0x1E2BF ),
      new Interval( 0x1E2FA, 0x1E2FE ), new Interval( 0x1E300, 0x1E4CF ), new Interval( 0x1E4FA, 0x1E7DF ),
      new Interval( 0x1E7E7, 0x1E7E7 ), new Interval( 0x1E7EC, 0x1E7EC ), new Interval( 0x1E7EF, 0x1E7EF ),
      new Interval( 0x1E7FF, 0x1E7FF ), new Interval( 0x1E8C5, 0x1E8C6 ), new Interval( 0x1E8D7, 0x1E8FF ),
      new Interval( 0x1E94C, 0x1E94F ), new Interval( 0x1E95A, 0x1E95D ), new Interval( 0x1E960, 0x1EC70 ),
      new Interval( 0x1ECB5, 0x1ED00 ), new Interval( 0x1ED3E, 0x1EDFF ), new Interval( 0x1EE04, 0x1EE04 ),
      new Interval( 0x1EE20, 0x1EE20 ), new Interval( 0x1EE23, 0x1EE23 ), new Interval( 0x1EE25, 0x1EE26 ),
      new Interval( 0x1EE28, 0x1EE28 ), new Interval( 0x1EE33, 0x1EE33 ), new Interval( 0x1EE38, 0x1EE38 ),
      new Interval( 0x1EE3A, 0x1EE3A ), new Interval( 0x1EE3C, 0x1EE41 ), new Interval( 0x1EE43, 0x1EE46 ),
      new Interval( 0x1EE48, 0x1EE48 ), new Interval( 0x1EE4A, 0x1EE4A ), new Interval( 0x1EE4C, 0x1EE4C ),
      new Interval( 0x1EE50, 0x1EE50 ), new Interval( 0x1EE53, 0x1EE53 ), new Interval( 0x1EE55, 0x1EE56 ),
      new Interval( 0x1EE58, 0x1EE58 ), new Interval( 0x1EE5A, 0x1EE5A ), new Interval( 0x1EE5C, 0x1EE5C ),
      new Interval( 0x1EE5E, 0x1EE5E ), new Interval( 0x1EE60, 0x1EE60 ), new Interval( 0x1EE63, 0x1EE63 ),
      new Interval( 0x1EE65, 0x1EE66 ), new Interval( 0x1EE6B, 0x1EE6B ), new Interval( 0x1EE73, 0x1EE73 ),
      new Interval( 0x1EE78, 0x1EE78 ), new Interval( 0x1EE7D, 0x1EE7D ), new Interval( 0x1EE7F, 0x1EE7F ),
      new Interval( 0x1EE8A, 0x1EE8A ), new Interval( 0x1EE9C, 0x1EEA0 ), new Interval( 0x1EEA4, 0x1EEA4 ),
      new Interval( 0x1EEAA, 0x1EEAA ), new Interval( 0x1EEBC, 0x1EEEF ), new Interval( 0x1EEF2, 0x1EFFF ),
      new Interval( 0x1F02C, 0x1F02F ), new Interval( 0x1F094, 0x1F09F ), new Interval( 0x1F0AF, 0x1F0B0 ),
      new Interval( 0x1F0C0, 0x1F0C0 ), new Interval( 0x1F0D0, 0x1F0D0 ), new Interval( 0x1F0F6, 0x1F0FF ),
      new Interval( 0x1F1AE, 0x1F1E5 ), new Interval( 0x1F203, 0x1F20F ), new Interval( 0x1F23C, 0x1F23F ),
      new Interval( 0x1F249, 0x1F24F ), new Interval( 0x1F252, 0x1F25F ), new Interval( 0x1F266, 0x1F2FF ),
      new Interval( 0x1F6D8, 0x1F6DB ), new Interval( 0x1F6ED, 0x1F6EF ), new Interval( 0x1F6FD, 0x1F6FF ),
      new Interval( 0x1F777, 0x1F77A ), new Interval( 0x1F7DA, 0x1F7DF ), new Interval( 0x1F7EC, 0x1F7EF ),
      new Interval( 0x1F7F1, 0x1F7FF ), new Interval( 0x1F80C, 0x1F80F ), new Interval( 0x1F848, 0x1F84F ),
      new Interval( 0x1F85A, 0x1F85F ), new Interval( 0x1F888, 0x1F88F ), new Interval( 0x1F8AE, 0x1F8AF ),
      new Interval( 0x1F8B2, 0x1F8FF ), new Interval( 0x1FA54, 0x1FA5F ), new Interval( 0x1FA6E, 0x1FA6F ),
      new Interval( 0x1FA7D, 0x1FA7F ), new Interval( 0x1FA89, 0x1FA8F ), new Interval( 0x1FABE, 0x1FABE ),
      new Interval( 0x1FAC6, 0x1FACD ), new Interval( 0x1FADC, 0x1FADF ), new Interval( 0x1FAE9, 0x1FAEF ),
      new Interval( 0x1FAF9, 0x1FAFF ), new Interval( 0x1FB93, 0x1FB93 ), new Interval( 0x1FBCB, 0x1FBEF ),
      new Interval( 0x1FBFA, 0x1FFFF ), new Interval( 0x2A6D7, 0x2F7FF ), new Interval( 0x2FA1E, 0xE0000 ),
      new Interval( 0xE0002, 0xE001F ), new Interval( 0xE0080, 0xE00FF ), new Interval( 0xE01F0, 0x10FFFD )
    };
    /* *INDENT-ON* */
    
    /* sorted list of non-overlapping intervals of non-characters */
    /* generated by
     *    uniset +WIDTH-W -cat=Cn -cat=Mn c
     */
    /* *INDENT-OFF* */
    /* generated by run-uniset_dbl 1.2 */
    static final Interval[] doublewidth = new Interval[] {
      new Interval( 0x1100, 0x115F ), new Interval( 0x231A, 0x231B ), new Interval( 0x2329, 0x232A ),
      new Interval( 0x23E9, 0x23EC ), new Interval( 0x23F0, 0x23F0 ), new Interval( 0x23F3, 0x23F3 ),
      new Interval( 0x25FD, 0x25FE ), new Interval( 0x2614, 0x2615 ), new Interval( 0x2648, 0x2653 ),
      new Interval( 0x267F, 0x267F ), new Interval( 0x2693, 0x2693 ), new Interval( 0x26A1, 0x26A1 ),
      new Interval( 0x26AA, 0x26AB ), new Interval( 0x26BD, 0x26BE ), new Interval( 0x26C4, 0x26C5 ),
      new Interval( 0x26CE, 0x26CE ), new Interval( 0x26D4, 0x26D4 ), new Interval( 0x26EA, 0x26EA ),
      new Interval( 0x26F2, 0x26F3 ), new Interval( 0x26F5, 0x26F5 ), new Interval( 0x26FA, 0x26FA ),
      new Interval( 0x26FD, 0x26FD ), new Interval( 0x2705, 0x2705 ), new Interval( 0x270A, 0x270B ),
      new Interval( 0x2728, 0x2728 ), new Interval( 0x274C, 0x274C ), new Interval( 0x274E, 0x274E ),
      new Interval( 0x2753, 0x2755 ), new Interval( 0x2757, 0x2757 ), new Interval( 0x2795, 0x2797 ),
      new Interval( 0x27B0, 0x27B0 ), new Interval( 0x27BF, 0x27BF ), new Interval( 0x2B1B, 0x2B1C ),
      new Interval( 0x2B50, 0x2B50 ), new Interval( 0x2B55, 0x2B55 ), new Interval( 0x2E80, 0x2E99 ),
      new Interval( 0x2E9B, 0x2EF3 ), new Interval( 0x2F00, 0x2FD5 ), new Interval( 0x2FF0, 0x3029 ),
      new Interval( 0x302E, 0x303E ), new Interval( 0x3041, 0x3096 ), new Interval( 0x309B, 0x30FF ),
      new Interval( 0x3105, 0x312F ), new Interval( 0x3131, 0x318E ), new Interval( 0x3190, 0x31E3 ),
      new Interval( 0x31EF, 0x321E ), new Interval( 0x3220, 0x3247 ), new Interval( 0x3250, 0x4DBF ),
      new Interval( 0x4E00, 0xA48C ), new Interval( 0xA490, 0xA4C6 ), new Interval( 0xA960, 0xA97C ),
      new Interval( 0xAC00, 0xD7A3 ), new Interval( 0xF900, 0xFAFF ), new Interval( 0xFE10, 0xFE19 ),
      new Interval( 0xFE30, 0xFE52 ), new Interval( 0xFE54, 0xFE66 ), new Interval( 0xFE68, 0xFE6B ),
      new Interval( 0xFF01, 0xFF60 ), new Interval( 0xFFE0, 0xFFE6 ), new Interval( 0x16FE0, 0x16FE3 ),
      new Interval( 0x16FF0, 0x16FF1 ), new Interval( 0x17000, 0x187F7 ), new Interval( 0x18800, 0x18CD5 ),
      new Interval( 0x18D00, 0x18D08 ), new Interval( 0x1AFF0, 0x1AFF3 ), new Interval( 0x1AFF5, 0x1AFFB ),
      new Interval( 0x1AFFD, 0x1AFFE ), new Interval( 0x1B000, 0x1B122 ), new Interval( 0x1B132, 0x1B132 ),
      new Interval( 0x1B150, 0x1B152 ), new Interval( 0x1B155, 0x1B155 ), new Interval( 0x1B164, 0x1B167 ),
      new Interval( 0x1B170, 0x1B2FB ), new Interval( 0x1F004, 0x1F004 ), new Interval( 0x1F0CF, 0x1F0CF ),
      new Interval( 0x1F18E, 0x1F18E ), new Interval( 0x1F191, 0x1F19A ), new Interval( 0x1F200, 0x1F202 ),
      new Interval( 0x1F210, 0x1F23B ), new Interval( 0x1F240, 0x1F248 ), new Interval( 0x1F250, 0x1F251 ),
      new Interval( 0x1F260, 0x1F265 ), new Interval( 0x1F300, 0x1F320 ), new Interval( 0x1F32D, 0x1F335 ),
      new Interval( 0x1F337, 0x1F37C ), new Interval( 0x1F37E, 0x1F393 ), new Interval( 0x1F3A0, 0x1F3CA ),
      new Interval( 0x1F3CF, 0x1F3D3 ), new Interval( 0x1F3E0, 0x1F3F0 ), new Interval( 0x1F3F4, 0x1F3F4 ),
      new Interval( 0x1F3F8, 0x1F43E ), new Interval( 0x1F440, 0x1F440 ), new Interval( 0x1F442, 0x1F4FC ),
      new Interval( 0x1F4FF, 0x1F53D ), new Interval( 0x1F54B, 0x1F54E ), new Interval( 0x1F550, 0x1F567 ),
      new Interval( 0x1F57A, 0x1F57A ), new Interval( 0x1F595, 0x1F596 ), new Interval( 0x1F5A4, 0x1F5A4 ),
      new Interval( 0x1F5FB, 0x1F64F ), new Interval( 0x1F680, 0x1F6C5 ), new Interval( 0x1F6CC, 0x1F6CC ),
      new Interval( 0x1F6D0, 0x1F6D2 ), new Interval( 0x1F6D5, 0x1F6D7 ), new Interval( 0x1F6DC, 0x1F6DF ),
      new Interval( 0x1F6EB, 0x1F6EC ), new Interval( 0x1F6F4, 0x1F6FC ), new Interval( 0x1F7E0, 0x1F7EB ),
      new Interval( 0x1F7F0, 0x1F7F0 ), new Interval( 0x1F90C, 0x1F93A ), new Interval( 0x1F93C, 0x1F945 ),
      new Interval( 0x1F947, 0x1F9FF ), new Interval( 0x1FA70, 0x1FA7C ), new Interval( 0x1FA80, 0x1FA88 ),
      new Interval( 0x1FA90, 0x1FABD ), new Interval( 0x1FABF, 0x1FAC5 ), new Interval( 0x1FACE, 0x1FADB ),
      new Interval( 0x1FAE0, 0x1FAE8 ), new Interval( 0x1FAF0, 0x1FAF8 ), new Interval( 0x20000, 0x2FFFD ),
      new Interval( 0x30000, 0x3FFFD )
    };
    /* *INDENT-ON* */
    
    static boolean Lookup(int cmp, Interval[] table) {
    	return bisearch(cmp, table, table.length - 1);
    }
    
	public static int mk_wcwidth(int cmp) {
		int result;

		/* test for 8-bit control characters */
		if (cmp == 0) {
			result = 0;
		} else if (cmp < 32 || (cmp >= 0x7f && cmp < 0xa0)) {
			result = -1;
		} else if (cmp == 0xad) {
			result = use_latin1;
		} else if (Lookup(cmp, formatting)) {
			/* treat formatting characters like control characters */
			result = -1;
		} else if (Lookup(cmp, combining)) {
			/* binary search in table of non-spacing characters */
			result = 0;
		} else {
			/* if we arrive here, cmp is not a combining or C0/C1 control character */
			result = 1;

			if (Lookup(cmp, doublewidth)) {
				result = 2;
			} else if (cmp >= unknowns[0].first && Lookup(cmp, unknowns)) {
				result = -1;
			}
		}
		return result;
	}
	
	public static int mk_wcswidth(String pwcs)
	{
	  return pwcs.codePoints().reduce(0, (a,b) -> a + mk_wcwidth(b));
	}

	/*
	 * The following functions are the same as mk_wcwidth() and
	 * mk_wcwidth_cjk(), except that spacing characters in the East Asian
	 * Ambiguous (A) category as defined in Unicode Technical Report #11
	 * have a column width of 2. This variant might be useful for users of
	 * CJK legacy encodings who want to migrate to UCS without changing
	 * the traditional terminal character-width behaviour. It is not
	 * otherwise recommended for general use.
	 */
	
	/* sorted list of non-overlapping intervals of East Asian Ambiguous
	   * characters, generated by
	   *
	   * uniset +WIDTH-A -cat=Me -cat=Mn -cat=Cf \
	   *    +E000..F8FF \
	   *    +F0000..FFFFD \
	   *    +100000..10FFFD  c
	   *
	   * "WIDTH-A" is a file extracted from EastAsianWidth.txt by selecting
	   * only those with width "A", and omitting:
	   *
	   *    0xAD
	   *    all lines with "COMBINING"
	   */
	  /* *INDENT-OFF* */
	  /* generated by run-uniset_cjk 1.5 */
	  static final Interval[] ambiguous = new Interval[] {
	    new Interval( 0x00A1, 0x00A1 ), new Interval( 0x00A4, 0x00A4 ), new Interval( 0x00A7, 0x00A8 ),
	    new Interval( 0x00AA, 0x00AA ), new Interval( 0x00AE, 0x00AE ), new Interval( 0x00B0, 0x00B4 ),
	    new Interval( 0x00B6, 0x00BA ), new Interval( 0x00BC, 0x00BF ), new Interval( 0x00C6, 0x00C6 ),
	    new Interval( 0x00D0, 0x00D0 ), new Interval( 0x00D7, 0x00D8 ), new Interval( 0x00DE, 0x00E1 ),
	    new Interval( 0x00E6, 0x00E6 ), new Interval( 0x00E8, 0x00EA ), new Interval( 0x00EC, 0x00ED ),
	    new Interval( 0x00F0, 0x00F0 ), new Interval( 0x00F2, 0x00F3 ), new Interval( 0x00F7, 0x00FA ),
	    new Interval( 0x00FC, 0x00FC ), new Interval( 0x00FE, 0x00FE ), new Interval( 0x0101, 0x0101 ),
	    new Interval( 0x0111, 0x0111 ), new Interval( 0x0113, 0x0113 ), new Interval( 0x011B, 0x011B ),
	    new Interval( 0x0126, 0x0127 ), new Interval( 0x012B, 0x012B ), new Interval( 0x0131, 0x0133 ),
	    new Interval( 0x0138, 0x0138 ), new Interval( 0x013F, 0x0142 ), new Interval( 0x0144, 0x0144 ),
	    new Interval( 0x0148, 0x014B ), new Interval( 0x014D, 0x014D ), new Interval( 0x0152, 0x0153 ),
	    new Interval( 0x0166, 0x0167 ), new Interval( 0x016B, 0x016B ), new Interval( 0x01CE, 0x01CE ),
	    new Interval( 0x01D0, 0x01D0 ), new Interval( 0x01D2, 0x01D2 ), new Interval( 0x01D4, 0x01D4 ),
	    new Interval( 0x01D6, 0x01D6 ), new Interval( 0x01D8, 0x01D8 ), new Interval( 0x01DA, 0x01DA ),
	    new Interval( 0x01DC, 0x01DC ), new Interval( 0x0251, 0x0251 ), new Interval( 0x0261, 0x0261 ),
	    new Interval( 0x02C4, 0x02C4 ), new Interval( 0x02C7, 0x02C7 ), new Interval( 0x02C9, 0x02CB ),
	    new Interval( 0x02CD, 0x02CD ), new Interval( 0x02D0, 0x02D0 ), new Interval( 0x02D8, 0x02DB ),
	    new Interval( 0x02DD, 0x02DD ), new Interval( 0x02DF, 0x02DF ), new Interval( 0x0391, 0x03A1 ),
	    new Interval( 0x03A3, 0x03A9 ), new Interval( 0x03B1, 0x03C1 ), new Interval( 0x03C3, 0x03C9 ),
	    new Interval( 0x0401, 0x0401 ), new Interval( 0x0410, 0x044F ), new Interval( 0x0451, 0x0451 ),
	    new Interval( 0x2010, 0x2010 ), new Interval( 0x2013, 0x2016 ), new Interval( 0x2018, 0x2019 ),
	    new Interval( 0x201C, 0x201D ), new Interval( 0x2020, 0x2022 ), new Interval( 0x2024, 0x2027 ),
	    new Interval( 0x2030, 0x2030 ), new Interval( 0x2032, 0x2033 ), new Interval( 0x2035, 0x2035 ),
	    new Interval( 0x203B, 0x203B ), new Interval( 0x203E, 0x203E ), new Interval( 0x2074, 0x2074 ),
	    new Interval( 0x207F, 0x207F ), new Interval( 0x2081, 0x2084 ), new Interval( 0x20AC, 0x20AC ),
	    new Interval( 0x2103, 0x2103 ), new Interval( 0x2105, 0x2105 ), new Interval( 0x2109, 0x2109 ),
	    new Interval( 0x2113, 0x2113 ), new Interval( 0x2116, 0x2116 ), new Interval( 0x2121, 0x2122 ),
	    new Interval( 0x2126, 0x2126 ), new Interval( 0x212B, 0x212B ), new Interval( 0x2153, 0x2154 ),
	    new Interval( 0x215B, 0x215E ), new Interval( 0x2160, 0x216B ), new Interval( 0x2170, 0x2179 ),
	    new Interval( 0x2189, 0x2189 ), new Interval( 0x2190, 0x2199 ), new Interval( 0x21B8, 0x21B9 ),
	    new Interval( 0x21D2, 0x21D2 ), new Interval( 0x21D4, 0x21D4 ), new Interval( 0x21E7, 0x21E7 ),
	    new Interval( 0x2200, 0x2200 ), new Interval( 0x2202, 0x2203 ), new Interval( 0x2207, 0x2208 ),
	    new Interval( 0x220B, 0x220B ), new Interval( 0x220F, 0x220F ), new Interval( 0x2211, 0x2211 ),
	    new Interval( 0x2215, 0x2215 ), new Interval( 0x221A, 0x221A ), new Interval( 0x221D, 0x2220 ),
	    new Interval( 0x2223, 0x2223 ), new Interval( 0x2225, 0x2225 ), new Interval( 0x2227, 0x222C ),
	    new Interval( 0x222E, 0x222E ), new Interval( 0x2234, 0x2237 ), new Interval( 0x223C, 0x223D ),
	    new Interval( 0x2248, 0x2248 ), new Interval( 0x224C, 0x224C ), new Interval( 0x2252, 0x2252 ),
	    new Interval( 0x2260, 0x2261 ), new Interval( 0x2264, 0x2267 ), new Interval( 0x226A, 0x226B ),
	    new Interval( 0x226E, 0x226F ), new Interval( 0x2282, 0x2283 ), new Interval( 0x2286, 0x2287 ),
	    new Interval( 0x2295, 0x2295 ), new Interval( 0x2299, 0x2299 ), new Interval( 0x22A5, 0x22A5 ),
	    new Interval( 0x22BF, 0x22BF ), new Interval( 0x2312, 0x2312 ), new Interval( 0x2460, 0x24E9 ),
	    new Interval( 0x24EB, 0x254B ), new Interval( 0x2550, 0x2573 ), new Interval( 0x2580, 0x258F ),
	    new Interval( 0x2592, 0x2595 ), new Interval( 0x25A0, 0x25A1 ), new Interval( 0x25A3, 0x25A9 ),
	    new Interval( 0x25B2, 0x25B3 ), new Interval( 0x25B6, 0x25B7 ), new Interval( 0x25BC, 0x25BD ),
	    new Interval( 0x25C0, 0x25C1 ), new Interval( 0x25C6, 0x25C8 ), new Interval( 0x25CB, 0x25CB ),
	    new Interval( 0x25CE, 0x25D1 ), new Interval( 0x25E2, 0x25E5 ), new Interval( 0x25EF, 0x25EF ),
	    new Interval( 0x2605, 0x2606 ), new Interval( 0x2609, 0x2609 ), new Interval( 0x260E, 0x260F ),
	    new Interval( 0x261C, 0x261C ), new Interval( 0x261E, 0x261E ), new Interval( 0x2640, 0x2640 ),
	    new Interval( 0x2642, 0x2642 ), new Interval( 0x2660, 0x2661 ), new Interval( 0x2663, 0x2665 ),
	    new Interval( 0x2667, 0x266A ), new Interval( 0x266C, 0x266D ), new Interval( 0x266F, 0x266F ),
	    new Interval( 0x269E, 0x269F ), new Interval( 0x26BF, 0x26BF ), new Interval( 0x26C6, 0x26CD ),
	    new Interval( 0x26CF, 0x26D3 ), new Interval( 0x26D5, 0x26E1 ), new Interval( 0x26E3, 0x26E3 ),
	    new Interval( 0x26E8, 0x26E9 ), new Interval( 0x26EB, 0x26F1 ), new Interval( 0x26F4, 0x26F4 ),
	    new Interval( 0x26F6, 0x26F9 ), new Interval( 0x26FB, 0x26FC ), new Interval( 0x26FE, 0x26FF ),
	    new Interval( 0x273D, 0x273D ), new Interval( 0x2776, 0x277F ), new Interval( 0x2B56, 0x2B59 ),
	    new Interval( 0x3248, 0x324F ), new Interval( 0xE000, 0xF8FF ), new Interval( 0xFFFD, 0xFFFD ),
	    new Interval( 0x1F100, 0x1F10A ), new Interval( 0x1F110, 0x1F12D ), new Interval( 0x1F130, 0x1F169 ),
	    new Interval( 0x1F170, 0x1F18D ), new Interval( 0x1F18F, 0x1F190 ), new Interval( 0x1F19B, 0x1F1AC ),
	    new Interval( 0xF0000, 0xFFFFD ), new Interval( 0x100000, 0x10FFFD )
	  };
	  /* *INDENT-ON* */

  public static int mk_wcwidth_cjk(int cmp)
  {
	  /* binary search in table of non-spacing characters */
	  if (Lookup(cmp, ambiguous))
	    return 2;

	  return mk_wcwidth(cmp);
  }
	
  public static int mk_wcswidth_cjk(String pwcs)
  {
    return pwcs.codePoints().reduce(0, (a,b) -> a + mk_wcwidth_cjk(b));
  }
}
