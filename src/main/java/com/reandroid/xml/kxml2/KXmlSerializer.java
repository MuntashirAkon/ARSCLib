/* Copyright (c) 2002,2003, Stefan Haustein, Oberhausen, Rhld., Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The  above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE. */

package com.reandroid.xml.kxml2;

import java.io.*;
import java.util.Arrays;
import java.util.Locale;
import org.xmlpull.v1.*;

public class KXmlSerializer implements XmlSerializer {

    private static final int BUFFER_LEN = 8192;
    private final char[] mText = new char[BUFFER_LEN];
    private int mPos;
    private Writer writer;
    private boolean pending;
    private int auto;
    private int depth;
    private String[] elementStack = new String[12];
    private int[] nspCounts = new int[4];
    private String[] nspStack = new String[8];
    private boolean[] indent = new boolean[4];
    private boolean firstAttributeWritten;
    private int indentAttributeReference;
    private boolean unicode;
    private String encoding;

    public KXmlSerializer(){
    }
    private void append(char c) throws IOException {
        if(mPos >= BUFFER_LEN){
            flushBuffer();
        }
        mText[mPos++] = c;
    }

    private void append(String str, int i, int length) throws IOException {
        while (length > 0){
            if(mPos == BUFFER_LEN){
                flushBuffer();
            }
            int batch = BUFFER_LEN - mPos;
            if(batch > length){
                batch = length;
            }
            str.getChars(i, i + batch, mText, mPos);
            i += batch;
            length -= batch;
            mPos += batch;
        }
    }

    private void appendSpace(int length) throws IOException {
        while (length > 0){
            if(mPos == BUFFER_LEN){
                flushBuffer();
            }
            int batch = BUFFER_LEN - mPos;
            if(batch > length){
                batch = length;
            }
            Arrays.fill(mText, mPos, mPos + batch, ' ');
            length -= batch;
            mPos += batch;
        }
    }

    private void append(String str) throws IOException {
        append(str, 0, str.length());
    }

    private void flushBuffer() throws IOException {
        if(mPos > 0){
            writer.write(mText, 0, mPos);
            writer.flush();
            mPos = 0;
        }
    }

    private void check(boolean close) throws IOException {
        if(!pending)
            return;

        depth++;
        pending = false;

        if(indent.length <= depth){
            boolean[] hlp = new boolean[depth + 4];
            System.arraycopy(indent, 0, hlp, 0, depth);
            indent = hlp;
        }
        indent[depth] = indent[depth - 1];

        for (int i = nspCounts[depth - 1]; i < nspCounts[depth]; i++){
            append(" xmlns");
            if(!nspStack[i * 2].isEmpty()){
                append(':');
                append(nspStack[i * 2]);
            }
            else if(getNamespace().isEmpty() && !nspStack[i * 2 + 1].isEmpty())
                throw new IllegalStateException("Cannot set default namespace for elements in no namespace");
            append("=\"");
            writeEscaped(nspStack[i * 2 + 1], '"');
            append('"');
        }

        if(nspCounts.length <= depth + 1){
            int[] hlp = new int[depth + 8];
            System.arraycopy(nspCounts, 0, hlp, 0, depth + 1);
            nspCounts = hlp;
        }

        nspCounts[depth + 1] = nspCounts[depth];
        if(close){
            append(" />");
        } else {
            append('>');
        }
    }

    private void writeEscaped(String s, int quot) throws IOException {
        for (int i = 0; i < s.length(); i++){
            char c = s.charAt(i);
            switch (c){
                case '\n':
                case '\r':
                case '\t':
                    if(quot == -1)
                        append(c);
                    else
                        append("&#"+((int) c)+';');
                    break;
                case '&' :
                    append("&amp;");
                    break;
                case '>' :
                    append("&gt;");
                    break;
                case '<' :
                    append("&lt;");
                    break;
                default:
                    if(c == quot){
                        append(c == '"' ? "&quot;" : "&apos;");
                        break;
                    }
                    boolean allowedInXml = (c >= 0x20 && c <= 0xd7ff) || (c >= 0xe000 && c <= 0xfffd);
                    if(allowedInXml){
                        if(unicode || c < 127){
                            append(c);
                        } else {
                            append("&#" + ((int) c) + ";");
                        }
                    } else if(Character.isHighSurrogate(c) && i < s.length() - 1){
                        writeSurrogate(c, s.charAt(i + 1));
                        ++i;
                    } else {
                        reportInvalidCharacter(c);
                    }
            }
        }
    }
    private static void reportInvalidCharacter(char ch){
        throw new IllegalArgumentException("Illegal character (U+" + Integer.toHexString((int) ch) + ")");
    }
    @Override
    public void docdecl(String dd) throws IOException {
        append("<!DOCTYPE");
        append(dd);
        append('>');
    }
    @Override
    public void endDocument() throws IOException {
        while (depth > 0){
            endTag(elementStack[depth * 3 - 3], elementStack[depth * 3 - 1]);
        }
        flush();
    }
    @Override
    public void entityRef(String name) throws IOException {
        check(false);
        append('&');
        append(name);
        append(';');
    }
    @Override
    public boolean getFeature(String name){
        return "http://xmlpull.org/v1/doc/features.html#indent-output"
                .equals(name) && indent[depth];
    }
    @Override
    public String getPrefix(String namespace, boolean create){
        try {
            return getPrefix(namespace, false, create);
        }
        catch (IOException e){
            throw new RuntimeException(e.toString());
        }
    }
    private String getPrefix(String namespace, boolean includeDefault, boolean create)
            throws IOException {
        int[] nspCounts = this.nspCounts;
        int depth = this.depth;
        String[] nspStack = this.nspStack;

        for (int i = nspCounts[depth + 1] * 2 - 2; i >= 0;i -= 2){
            if(nspStack[i + 1].equals(namespace)
                    && (includeDefault 
                    || !nspStack[i].isEmpty())){
                String cand = nspStack[i];
                for (int j = i + 2; j < nspCounts[depth + 1] * 2; j++){
                    if(nspStack[j].equals(cand)){
                        cand = null;
                        break;
                    }
                }
                if(cand != null){
                    return cand;
                }
            }
        }
        if(!create){
            return null;
        }

        String prefix;

        if(namespace.isEmpty()) {
            prefix = "";
        }else {
            do {
                prefix = "n" + (auto++);
                for (int i = nspCounts[depth + 1] * 2 - 2;i >= 0;i -= 2){
                    if(prefix.equals(nspStack[i])){
                        prefix = null;
                        break;
                    }
                }
            }
            while (prefix == null);
        }

        boolean p = pending;
        pending = false;
        setPrefix(prefix, namespace);
        pending = p;
        return prefix;
    }

    @Override
    public Object getProperty(String name){
        throw new RuntimeException("Unsupported property: "+name);
    }
    @Override
    public void ignorableWhitespace(String s) throws IOException {
        text(s);
    }
    @Override
    public void setFeature(String name, boolean value){
        if("http://xmlpull.org/v1/doc/features.html#indent-output".equals(name)){
            indent[depth] = value;
            firstAttributeWritten = false;
        }else {
            throw new RuntimeException("Unsupported Feature: "+name);
        }
    }
    @Override
    public void setProperty(String name, Object value){
        throw new RuntimeException("Unsupported Property:" + value);
    }
    @Override
    public void setPrefix(String prefix, String namespace)
            throws IOException {

        check(false);
        if(prefix == null) {
            prefix = "";
        }
        if(namespace == null) {
            namespace = "";
        }
        String defined = getPrefix(namespace, true, false);
        if(prefix.equals(defined)) {
            return;
        }

        int pos = (nspCounts[depth + 1]++) << 1;

        if(nspStack.length < pos + 1){
            String[] hlp = new String[nspStack.length + 16];
            System.arraycopy(nspStack, 0, hlp, 0, pos);
            nspStack = hlp;
        }

        nspStack[pos++] = prefix;
        nspStack[pos] = namespace;
    }

    public void setOutput(Writer writer){
        this.writer = writer;
        nspCounts[0] = 2;
        nspCounts[1] = 2;
        nspStack[0] = "";
        nspStack[1] = "";
        nspStack[2] = "xml";
        nspStack[3] = "http://www.w3.org/XML/1998/namespace";
        pending = false;
        auto = 0;
        depth = 0;

        unicode = false;
    }
    @Override
    public void setOutput(OutputStream os, String encoding)
            throws IOException {
        if(os == null) {
            throw new IllegalArgumentException("os == null");
        }
        setOutput(encoding == null
                        ? new OutputStreamWriter(os)
                        : new OutputStreamWriter(os, encoding));
        this.encoding = encoding;
        if(encoding != null && encoding.toLowerCase(Locale.US).startsWith("utf")){
            unicode = true;
        }
    }
    @Override
    public void startDocument(String encoding, Boolean standalone) throws IOException {
        append("<?xml version='1.0' ");

        if(encoding != null){
            this.encoding = encoding;
            if(encoding.toLowerCase(Locale.US).startsWith("utf")){
                unicode = true;
            }
        }
        if(this.encoding != null){
            append("encoding='");
            append(this.encoding);
            append("' ");
        }
        if(standalone != null){
            append("standalone='");
            append(standalone ? "yes" : "no");
            append("' ");
        }
        append("?>");
    }
    @Override
    public XmlSerializer startTag(String namespace, String name)
            throws IOException {
        check(false);
        firstAttributeWritten = false;
        indentAttributeReference = 0;
        if(indent[depth]){
            append('\r');
            append('\n');
            int spaceLength = 2 * depth;
            appendSpace(spaceLength);
            indentAttributeReference = spaceLength;
        }
        int esp = depth * 3;
        if(elementStack.length < esp + 3){
            String[] hlp = new String[elementStack.length + 12];
            System.arraycopy(elementStack, 0, hlp, 0, esp);
            elementStack = hlp;
        }
        String prefix = namespace == null?
                "" : getPrefix(namespace, true, true);

        if(namespace != null && namespace.isEmpty()){
            for (int i = nspCounts[depth]; i < nspCounts[depth + 1]; i++){
                if(nspStack[i * 2].isEmpty() && !nspStack[i * 2 + 1].isEmpty()){
                    throw new IllegalStateException("Cannot set default namespace for elements in no namespace");
                }
            }
        }
        elementStack[esp++] = namespace;
        elementStack[esp++] = prefix;
        elementStack[esp] = name;
        append('<');
        indentAttributeReference += 1;
        if(!prefix.isEmpty()){
            append(prefix);
            append(':');
            indentAttributeReference += prefix.length() + 1;
        }
        append(name);
        int len = name.length();
        if(len > 20){
            len = 20;
        }
        indentAttributeReference += len;
        pending = true;
        return this;
    }
    @Override
    public XmlSerializer attribute(String namespace, String name, String value)
            throws IOException {
        if(!pending) {
            throw new IllegalStateException("illegal position for attribute");
        }
        if(namespace == null) {
            namespace = "";
        }
        String prefix = namespace.isEmpty() ?
                "" : getPrefix(namespace, false, true);
        attributeIndent();
        append(' ');
        if(!prefix.isEmpty()){
            append(prefix);
            append(':');
        }
        append(name);
        append('=');
        char q = value.indexOf('"') == -1 ? '"' : '\'';
        append(q);
        writeEscaped(value, q);
        append(q);
        firstAttributeWritten = true;
        return this;
    }
    @Override
    public void flush() throws IOException {
        check(false);
        flushBuffer();
    }
    @Override
    public XmlSerializer endTag(String namespace, String name)throws IOException {
        if(!pending) {
            depth--;
        }
        if((namespace == null
                && elementStack[depth * 3] != null)
                || (namespace != null
                && !namespace.equals(elementStack[depth * 3]))
                || !elementStack[depth * 3 + 2].equals(name)) {
            throw new IllegalArgumentException("</{"+namespace+"}"+name+"> does not match start");
        }

        if(pending){
            check(true);
            depth--;
        }
        else {
            if(indent[depth + 1]){
                append('\r');
                append('\n');
                appendSpace(2 * depth);
            }
            append("</");
            String prefix = elementStack[depth * 3 + 1];
            if(!prefix.isEmpty()){
                append(prefix);
                append(':');
            }
            append(name);
            append('>');
        }

        nspCounts[depth + 1] = nspCounts[depth];
        return this;
    }
    @Override
    public String getNamespace(){
        return getDepth() == 0 ? null : elementStack[getDepth() * 3 - 3];
    }
    @Override
    public String getName(){
        return getDepth() == 0 ? null : elementStack[getDepth() * 3 - 1];
    }
    @Override
    public int getDepth(){
        return pending ? depth + 1 : depth;
    }
    @Override
    public XmlSerializer text(String text) throws IOException {
        check(false);
        indent[depth] = false;
        writeEscaped(text, -1);
        return this;
    }
    @Override
    public XmlSerializer text(char[] text, int start, int len)
            throws IOException {
        text(new String(text, start, len));
        return this;
    }
    @Override
    public void cdsect(String data) throws IOException {
        check(false);
        data = data.replace("]]>", "]]]]><![CDATA[>");
        append("<![CDATA[");
        for (int i = 0; i < data.length(); ++i){
            char ch = data.charAt(i);
            boolean allowedInCdata = (ch >= 0x20 && ch <= 0xd7ff) ||
                    (ch == '\t' || ch == '\n' || ch == '\r') ||
                    (ch >= 0xe000 && ch <= 0xfffd);
            if(allowedInCdata){
                append(ch);
            } else if(Character.isHighSurrogate(ch) && i < data.length() - 1){
                // Character entities aren't valid in CDATA, so break out for this.
                append("]]>");
                writeSurrogate(ch, data.charAt(++i));
                append("<![CDATA[");
            } else {
                reportInvalidCharacter(ch);
            }
        }
        append("]]>");
    }

    private void writeSurrogate(char high, char low) throws IOException {
        if(!Character.isLowSurrogate(low)){
            throw new IllegalArgumentException("Bad surrogate pair (U+" + Integer.toHexString((int) high) +
                    " U+" + Integer.toHexString((int) low) + ")");
        }
        int codePoint = Character.toCodePoint(high, low);
        append("&#" + codePoint + ";");
    }
    @Override
    public void comment(String comment) throws IOException {
        check(false);
        append("<!--");
        append(comment);
        append("-->");
    }
    @Override
    public void processingInstruction(String pi)
            throws IOException {
        check(false);
        append("<?");
        append(pi);
        append("?>");
    }

    private void attributeIndent() throws IOException {
        if(!firstAttributeWritten || !indent[depth]){
            return;
        }
        int length = this.indentAttributeReference;
        if(length <= 0){
            return;
        }
        append('\r');
        append('\n');
        appendSpace(length);
    }
}
