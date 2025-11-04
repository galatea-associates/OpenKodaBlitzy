package com.openkoda.uicomponent;


import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * Lightweight JavaScript parser that extracts ES module exported function signatures using regex patterns.
 * <p>
 * Parses JavaScript source code to identify exported function declarations. Supports three function 
 * declaration patterns: standard function declarations, assignment with function keyword, and arrow functions.
 * Extracts function names and parameter lists, returning normalized signatures in 'name(args)' format.

 * <p>
 * Uses precompiled regex patterns for performance. Not a full JavaScript AST parser - suitable for 
 * simple signature discovery in well-formatted code.

 * <p>
 * Example usage:
 * <pre>
 * JsParser parser = new JsParser();
 * List&lt;String&gt; signatures = parser.getFunctions("export function test(a, b) {}");
 * // Returns: ["test(a,b)"]
 * </pre>

 *
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 * @see JsPattern
 */
@Component
public class JsParser {

    /**
     * Extracts all exported function signatures from JavaScript source code.
     * <p>
     * Iterates through all JsPattern enum values (function_standard, function_equals, function_lambda),
     * applies each pattern's regex to the code, and collects all matches. Each match is normalized:
     * function name extracted from nameGroup, parameters extracted from argsGroup with spaces removed.
     * Returns accumulated list of all discovered exported functions.

     * <p>
     * Example:
     * <pre>
     * Input: "export function test(a, b) {}" returns ["test(a,b)"]
     * Input: "export const fn = (x) =&gt; x" returns ["fn(x)"]
     * </pre>

     *
     * @param code JavaScript source code to parse
     * @return List of function signatures in 'name(args)' format (e.g., ['myFunc(a,b)', 'otherFunc()'])
     */
    public List<String> getFunctions(String code){
        List<String> functions = new ArrayList<>();
        for(JsPattern jsPattern : JsPattern.values()){
            addFunctions(jsPattern, code, functions);
        }
        return functions;
    }

    /**
     * Applies single JsPattern to code and adds discovered function signatures to accumulator list.
     * <p>
     * Creates Matcher from jsPattern.getPattern(), iterates all matches via matcher.find(). 
     * For each match, extracts function name from matcher.group(nameGroup) and parameters from 
     * matcher.group(argsGroup), trims whitespace, removes spaces from parameter list, constructs 
     * 'name(args)' format, and adds to functions list.

     *
     * @param jsPattern Pattern to apply (function_standard, function_equals, or function_lambda)
     * @param code JavaScript source code to search
     * @param functions Accumulator list for discovered function signatures (modified in-place)
     */
    private  void addFunctions(JsPattern jsPattern, String code, List<String> functions) {
        Matcher matcher = jsPattern.getPattern().matcher(code);
        while(matcher.find()){
            functions.add(matcher.group(jsPattern.getNameGroup()).trim() + "(" + matcher.group(jsPattern.getArgsGroup()).trim().replaceAll(" ","") + ")");
        }
    }

    /**
     * Enum of precompiled regex patterns for JavaScript exported function declarations.
     * <p>
     * Defines three patterns for different function declaration styles:

     * <ul>
     * <li>function_standard: Matches 'export function name(params)' style</li>
     * <li>function_equals: Matches 'export const/var/let name = function(params)' style</li>
     * <li>function_lambda: Matches 'export const/var/let name = (params) =&gt;' arrow function style</li>
     * </ul>
     * <p>
     * Each pattern captures function name in one regex group and parameters in another.
     * Groups are accessed via nameGroup and argsGroup indices.

     */
    private enum JsPattern {
        /**
         * Pattern for standard function declarations: export function name(params).
         * <p>
         * Pattern: "export\\s+function\\s+([^)]*)\\s*\\(([^)]*)"<br>
         * Name group: 1 (function name)<br>
         * Args group: 2 (parameter list)

         */
        function_standard(compile("export\\s+function\\s+([^)]*)\\s*\\(([^)]*)"), 1 ,2),
        
        /**
         * Pattern for function keyword assignments: export const/var/let name = function(params).
         * <p>
         * Pattern: "export\\s+(const|var|let)\\s+([^=]*)\\s*=\\s*function\\s*\\(([^)]*)\\)"<br>
         * Name group: 2 (function name, group 1 is const/var/let)<br>
         * Args group: 3 (parameter list)

         */
        function_equals(compile("export\\s+(const|var|let)\\s+([^=]*)\\s*=\\s*function\\s*\\(([^)]*)\\)"), 2 ,3),
        
        /**
         * Pattern for arrow function assignments: export const/var/let name = (params) =&gt;.
         * <p>
         * Pattern: "export\\s+(const|var|let)\\s+([^=]*)\\s*=\\s*\\(*([^)]*)\\)*\\s*=&gt;"<br>
         * Name group: 2 (function name, group 1 is const/var/let)<br>
         * Args group: 3 (parameter list)

         */
        function_lambda(compile("export\\s+(const|var|let)\\s+([^=]*)\\s*=\\s*\\(*([^)]*)\\)*\\s*=>"), 2 ,3);

        /** Precompiled regex Pattern for function signature matching. */
        final Pattern pattern;
        
        /** Regex capture group index for function name extraction. */
        final Integer nameGroup;
        
        /** Regex capture group index for function parameter list extraction. */
        final Integer argsGroup;

        /**
         * Constructs JsPattern with regex pattern and capture group indices.
         *
         * @param pattern Precompiled Pattern for function signature matching
         * @param nameGroup Capture group index for function name (1-based)
         * @param argsGroup Capture group index for parameter list (1-based)
         */
        JsPattern(Pattern pattern, Integer nameGroup, Integer argsGroup) {
            this.pattern = pattern;
            this.nameGroup = nameGroup;
            this.argsGroup = argsGroup;
        }

        /**
         * Returns the precompiled regex Pattern for this function declaration style.
         *
         * @return Compiled Pattern instance
         */
        Pattern getPattern() {
            return pattern;
        }

        /**
         * Returns the regex capture group index for extracting function name.
         *
         * @return Group index (1-based) for function name capture
         */
        Integer getNameGroup() {
            return nameGroup;
        }

        /**
         * Returns the regex capture group index for extracting parameter list.
         *
         * @return Group index (1-based) for parameter list capture
         */
        Integer getArgsGroup() {
            return argsGroup;
        }
    }
}
