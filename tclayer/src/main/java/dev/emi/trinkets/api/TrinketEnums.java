/*
 * MIT License
 *
 * Copyright (c) 2019 Emily Rose Ploszaj
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.emi.trinkets.api;

public class TrinketEnums {

    public enum DropRule {
        KEEP, DROP, DESTROY, DEFAULT;

        static public boolean has(String name) {
            DropRule[] rules = DropRule.values();

            for (DropRule rule : rules) {
                if (rule.toString().equals(name)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static io.wispforest.accessories.api.DropRule convert(DropRule dropRule){
        return switch (dropRule){
            case KEEP -> io.wispforest.accessories.api.DropRule.KEEP;
            case DROP -> io.wispforest.accessories.api.DropRule.DROP;
            case DESTROY -> io.wispforest.accessories.api.DropRule.DESTROY;
            case DEFAULT -> io.wispforest.accessories.api.DropRule.DEFAULT;
        };
    }

    public static DropRule convert(io.wispforest.accessories.api.DropRule dropRule){
        return switch (dropRule){
            case KEEP -> DropRule.KEEP;
            case DROP -> DropRule.DROP;
            case DESTROY -> DropRule.DESTROY;
            case DEFAULT -> DropRule.DEFAULT;
        };
    }
}
