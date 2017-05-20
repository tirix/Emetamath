package org.json;

import java.io.IOException;
import java.io.Writer;
import java.util.BitSet;

/*
Copyright (c) 2006 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

/**
 * JSONWriter provides a quick and convenient way of producing JSON text. The
 * texts produced strictly conform to JSON syntax rules. No whitespace is added,
 * so the results are ready for transmission or storage. Each instance of
 * JSONWriter can produce one JSON text.
 * <p>
 * A JSONWriter instance provides a <code>value</code> method for appending
 * values to the text, and a <code>key</code> method for adding keys before
 * values in objects. There are <code>array</code> and <code>endArray</code>
 * methods that make and bound array values, and <code>object</code> and
 * <code>endObject</code> methods which make and bound object values. All of
 * these methods return the JSONWriter instance, permitting a cascade style. For
 * example,
 *
 * <pre>
 * new JSONWriter(myWriter).object().key("JSON").value("Hello, World!")
 *     .endObject();
 * </pre>
 *
 * which writes
 *
 * <pre>
 * {"JSON":"Hello, World!"}
 * </pre>
 * <p>
 * The first method called must be <code>array</code> or <code>object</code>.
 * There are no methods for adding commas or colons. JSONWriter adds them for
 * you.
 * <p>
 * This can sometimes be easier than using a JSONObject to build a string.
 *
 * @author JSON.org
 * @version 2015-12-09
 */
public class JSONWriter {
    /**
     * The current mode. Values: 'a' (array), 'A' (array after first element),
     * 'i' (initial/done), 'k' (object: key), 'o' (object: value), 'K' (object:
     * key after first element). The stack top is completely determined by the
     * mode: it is empty in modes 'i', 'd', true (object) in modes 'k', 'o',
     * 'K', and false (array) in modes 'a', 'A'.
     */
    protected char mode;

    /**
     * The object/array stack. Stored as a BitSet, where objects are true and
     * arrays are false.
     */
    private final BitSet stack;

    /**
     * The stack top index. A value of 0 indicates that the stack is empty.
     */
    private int top;

    /**
     * The writer that will receive the output.
     */
    protected Writer writer;

    /**
     * Make a fresh JSONWriter. It can be used to build one JSON text.
     *
     * @param w The base writer
     */
    public JSONWriter(final Writer w) {
        mode = 'i';
        stack = new BitSet();
        top = 0;
        writer = w;
    }

    /**
     * Append a value.
     *
     * @param string A string value.
     * @return this
     * @throws JSONException If the value is out of sequence.
     */
    private JSONWriter append(final String string) throws JSONException {
        if (string == null)
            throw new JSONException("Null pointer");
        if (mode == 'd')
            throw new JSONException("Value out of sequence.");
        try {
            if (mode == 'A' || mode == 'K')
                writer.write(',');
            else if (mode == 'o')
                writer.write(':');
            writer.write(string);
        } catch (final IOException e) {
            throw new JSONException(e);
        }
        switch (mode) {
            case 'a':
                mode = 'A';
                break;
            case 'k':
            case 'K':
                mode = 'o';
                break;
            case 'o':
                mode = 'K';
                break;
            case 'i':
                mode = 'd';
                break;
        }
        return this;
    }

    /**
     * Begin appending a new array. All values until the balancing
     * <code>endArray</code> will be appended to this array. The
     * <code>endArray</code> method must be called to mark the array's end.
     *
     * @return this
     * @throws JSONException If the nesting is too deep, or if the object is
     *             started in the wrong place (for example as a key or after the
     *             end of the outermost array or object).
     */
    public JSONWriter array() throws JSONException {
        if (mode == 'i' || mode == 'o' || mode == 'a' || mode == 'A') {
            append("[");
            push(false);
            return this;
        }
        throw new JSONException("Misplaced array.");
    }

    /**
     * End something.
     *
     * @param c Closing character
     * @return this
     * @throws JSONException If unbalanced.
     */
    private JSONWriter end(final char c) throws JSONException {
        pop();
        try {
            writer.write(c);
        } catch (final IOException e) {
            throw new JSONException(e);
        }
        return this;
    }

    /**
     * End an array. This method most be called to balance calls to
     * <code>array</code>.
     *
     * @return this
     * @throws JSONException If incorrectly nested.
     */
    public JSONWriter endArray() throws JSONException {
        if (mode != 'a' && mode != 'A')
            throw new JSONException("Misplaced endArray.");
        return end(']');
    }

    /**
     * End an object. This method most be called to balance calls to
     * <code>object</code>.
     *
     * @return this
     * @throws JSONException If incorrectly nested.
     */
    public JSONWriter endObject() throws JSONException {
        if (mode != 'k' && mode != 'K')
            throw new JSONException("Misplaced endObject.");
        return end('}');
    }

    /**
     * End an object or array. This method most be called to balance calls to
     * <code>object</code> or <code>array</code>.
     *
     * @return this
     * @throws JSONException If incorrectly nested.
     */
    public JSONWriter end() throws JSONException {
        char bracket;
        switch (mode) {
            case 'a':
            case 'A':
                bracket = ']';
                break;
            case 'k':
            case 'K':
                bracket = '}';
                break;
            default:
                throw new JSONException("Misplaced end.");
        }
        return end(bracket);
    }

    /**
     * Append a key. The key will be associated with the next value. In an
     * object, every value must be preceded by a key.
     *
     * @param string A key string.
     * @return this
     * @throws JSONException If the key is out of place. For example, keys do
     *             not belong in arrays or if the key is null.
     */
    public JSONWriter key(final String string) throws JSONException {
        if (string == null)
            throw new JSONException("Null key.");
        if (mode == 'k' || mode == 'K')
            return append(JSONObject.quote(string));
        throw new JSONException("Misplaced key.");
    }

    /**
     * Begin appending a new object. All keys and values until the balancing
     * <code>endObject</code> will be appended to this object. The
     * <code>endObject</code> method must be called to mark the object's end.
     *
     * @return this
     * @throws JSONException If the nesting is too deep, or if the object is
     *             started in the wrong place (for example as a key or after the
     *             end of the outermost array or object).
     */
    public JSONWriter object() throws JSONException {
        if (mode == 'i' || mode == 'o' || mode == 'a' || mode == 'A') {
            append("{");
            push(true);
            return this;
        }
        throw new JSONException("Misplaced object.");

    }

    /**
     * Pop an array or object scope.
     *
     * @throws JSONException If nesting is wrong.
     */
    private void pop() throws JSONException {
        if (top <= 0)
            throw new JSONException("Nesting error.");
        top--;
        mode = top == 0 ? 'd' : stack.get(top - 1) ? 'K' : 'A';
    }

    /**
     * Push an array or object scope.
     *
     * @param b The scope to open.
     * @throws JSONException If nesting is too deep.
     */
    private void push(final boolean b) throws JSONException {
        stack.set(top++, b);
        mode = b ? 'k' : 'a';
    }

    /**
     * Append either the value <code>true</code> or the value <code>false</code>
     * .
     *
     * @param b A boolean.
     * @return this
     * @throws JSONException If something goes wrong
     */
    public JSONWriter value(final boolean b) throws JSONException {
        return append(b ? "true" : "false");
    }

    /**
     * Append a double value.
     *
     * @param d A double.
     * @return this
     * @throws JSONException If the number is not finite.
     */
    public JSONWriter value(final double d) throws JSONException {
        return value(new Double(d));
    }

    /**
     * Append a long value.
     *
     * @param l A long.
     * @return this
     * @throws JSONException If something goes wrong
     */
    public JSONWriter value(final long l) throws JSONException {
        return append(Long.toString(l));
    }

    /**
     * Append an object value.
     *
     * @param object The object to append. It can be null, or a Boolean, Number,
     *            String, JSONObject, or JSONArray, or an object that implements
     *            JSONString.
     * @return this
     * @throws JSONException If the value is out of sequence.
     */
    public JSONWriter value(final Object object) throws JSONException {
        return append(JSONObject.valueToString(object));
    }
}
