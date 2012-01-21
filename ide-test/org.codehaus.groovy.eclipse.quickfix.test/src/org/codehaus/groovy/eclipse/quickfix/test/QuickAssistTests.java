/*
 * Copyright 2011 SpringSource, a division of VMware, Inc
 * 
 * andrew - Initial API and implementation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.eclipse.quickfix.test;

import java.lang.reflect.InvocationTargetException;

import org.codehaus.groovy.eclipse.quickassist.AbstractGroovyCompletionProposal;
import org.codehaus.groovy.eclipse.quickassist.ConvertToClosureCompletionProposal;
import org.codehaus.groovy.eclipse.quickassist.ConvertToMethodCompletionProposal;
import org.codehaus.groovy.eclipse.quickassist.ConvertToMultiLineStringCompletionProposal;
import org.codehaus.groovy.eclipse.quickassist.ConvertToSingleLineStringCompletionProposal;
import org.codehaus.groovy.eclipse.quickassist.RemoveUnnecessarySemicolonsCompletionProposal;
import org.codehaus.groovy.eclipse.quickassist.SwapOperandsCompletionProposal;
import org.codehaus.groovy.eclipse.test.EclipseTestCase;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

/**
 * tests for the {@link ConvertToMethodCompletionProposal} class
 * @author Andrew Eisenberg
 * @created Oct 28, 2011
 */
public class QuickAssistTests extends EclipseTestCase {
    
    
    public void testConvertToClosure1() throws Exception {
        assertConversion("def x()  { }", "def x = { }", "x", ConvertToClosureCompletionProposal.class);
    }
    
    public void testConvertToClosure2() throws Exception {
        assertConversion("class X { \ndef x()  { } }", "class X { \ndef x = { } }", "x", ConvertToClosureCompletionProposal.class);
    }
    
    public void testConvertToClosure3() throws Exception {
        assertConversion("def x(a)  { }", "def x = { a -> }", "x", ConvertToClosureCompletionProposal.class);
    }
    

    public void testConvertToClosure4() throws Exception {
        assertConversion("def x(int a, int b)  { }", "def x = { int a, int b -> }", "x", ConvertToClosureCompletionProposal.class);
    }
    
    public void testConvertToClosure5() throws Exception {
        assertConversion("def x(int a, int b)  { fdafsd }", "def x = { int a, int b -> fdafsd }", "x", ConvertToClosureCompletionProposal.class);
    }
    
    public void testConvertToClosure6() throws Exception {
        assertConversion("def x(int a, int b)\n { fdafsd }", "def x = { int a, int b -> fdafsd }", "x", ConvertToClosureCompletionProposal.class);
    }
    
    public void testConvertToClosure7() throws Exception {
        assertConversion("def x(int a, int b   )\n { fdafsd }", "def x = { int a, int b    -> fdafsd }", "x", ConvertToClosureCompletionProposal.class);
    }
    
    public void testConvertToClosure8() throws Exception {
        assertConversion("def x   (int a, int b   )\n { fdafsd }", "def x    = { int a, int b    -> fdafsd }", "x", ConvertToClosureCompletionProposal.class);
    }
    
    public void testConvertToClosure9() throws Exception {
        assertConversion("def x(int a, int b)  {\n  fdsafds }", "def x = { int a, int b ->\n  fdsafds }", "x", ConvertToClosureCompletionProposal.class);
    }
    
    public void testConvertToClosure10() throws Exception {
        assertConversion("def xxxx(int a, int b)  {\n  fdsafds }", "def xxxx = { int a, int b ->\n  fdsafds }", "x", ConvertToClosureCompletionProposal.class);
    }
    
    public void testConvertToClosure11() throws Exception {
        assertConversion("def \"xx  xx\"(int a, int b)  {\n  fdsafds }", "def \"xx  xx\" = { int a, int b ->\n  fdsafds }", "x", ConvertToClosureCompletionProposal.class);
    }
    
    // convert to method must be wrapped inside of a class declaration
    public void testConvertToMethod1() throws Exception {
        assertConversion("class X { \ndef x = { } }", "class X { \ndef x() { } }", "x", ConvertToMethodCompletionProposal.class);
    }
    
    public void testConvertToMethod3() throws Exception {
        assertConversion("class X { \ndef x = { a ->  } }", "class X { \ndef x(a) {  } }", "x", ConvertToMethodCompletionProposal.class);
    }
    

    public void testConvertToMethod4() throws Exception {
        assertConversion("class X { \ndef x = {int a, int b -> } }", "class X { \ndef x(int a, int b) { } }", "x", ConvertToMethodCompletionProposal.class);
    }
    
    public void testConvertToMethod5() throws Exception {
        assertConversion("class X { \ndef x = {int a, int b -> fdafsd } }", "class X { \ndef x(int a, int b) { fdafsd } }", "x", ConvertToMethodCompletionProposal.class);
    }
    
    public void testConvertToMethod6() throws Exception {
        assertConversion("class X { \ndef x = {int a, int b -> fdafsd } }", "class X { \ndef x(int a, int b) { fdafsd } }", "x", ConvertToMethodCompletionProposal.class);
    }
    
    public void testConvertToMethod7() throws Exception {
        assertConversion("class X { \ndef x = {int a, int b   -> fdafsd } }", "class X { \ndef x(int a, int b) { fdafsd } }", "x", ConvertToMethodCompletionProposal.class);
    }
    
    public void testConvertToMethod8() throws Exception {
        assertConversion("class X { \ndef x    = {    int a, int b   -> fdafsd } }", "class X { \ndef x(int a, int b) { fdafsd } }", "x", ConvertToMethodCompletionProposal.class);
    }
    
    public void testConvertToMethod9() throws Exception {
        assertConversion("class X { \ndef x = {int a, int b\n ->\n  fdsafds } }", "class X { \ndef x(int a, int b) {\n  fdsafds } }", "x", ConvertToMethodCompletionProposal.class);
    }
    
    public void testConvertToMethod10() throws Exception {
        assertConversion("class X { \ndef xxxx = {int a, int b -> \n  fdsafds } }", "class X { \ndef xxxx(int a, int b) { \n  fdsafds } }", "x", ConvertToMethodCompletionProposal.class);
    }
    
    public void testConvertToMethod11() throws Exception {
        assertConversion("class X { \ndef xxxx = {int a, int b ->\n  fdsafds } }", "class X { \ndef xxxx(int a, int b) {\n  fdsafds } }", "x", ConvertToMethodCompletionProposal.class);
    }
    
    public void testConvertToMultiLine1() throws Exception {
        assertConversion("\"fadfsad\\n\\t' \\\"\\nggggg\"", 
        		"\"\"\"fadfsad\n\t' \"\nggggg\"\"\"", "f", ConvertToMultiLineStringCompletionProposal.class);
    }
    
    public void testConvertToMultiLine2() throws Exception {
        assertConversion("'fadfsad\\n\\t\\' \"\\nggggg'", 
                "'''fadfsad\n\t' \"\nggggg'''", "f", ConvertToMultiLineStringCompletionProposal.class);
    }
    
    public void testConvertToSingleLine1() throws Exception {
        assertConversion("\"\"\"fadfsad\n\t' \"\nggggg\"\"\"",
                "\"fadfsad\\n\\t' \\\"\\nggggg\"", "f", ConvertToSingleLineStringCompletionProposal.class);
    }
    
    public void testConvertToSingleLine2() throws Exception {
        assertConversion("'''fadfsad\n\t' \"\nggggg'''", 
                "'fadfsad\\n\\t\\' \"\\nggggg'", "f", ConvertToSingleLineStringCompletionProposal.class);
    }
    
    public void testRemoveUnnecessarySemicolons1() throws Exception {
    	 String original = "def a = 1;";
		assertConversion(original, "def a = 1", original, RemoveUnnecessarySemicolonsCompletionProposal.class);
    }
    
    public void testRemoveUnnecessarySemicolons2() throws Exception {
        String original = "def z = 1;def a = 1;";
        String expected = "def z = 1;def a = 1";
       assertConversion(original, expected, null, RemoveUnnecessarySemicolonsCompletionProposal.class);
   }
    
    
	public void testSwapOperands1() throws Exception {
		assertConversion("if (c && ba)", "if (ba && c)", 7, 1,
				SwapOperandsCompletionProposal.class);
	}

	public void testSwapOperands2() throws Exception {
		assertConversion("if (c && ba && hello)", "if (hello && c && ba)", 13,
				1, SwapOperandsCompletionProposal.class);
	}

	public void testSwapOperands3() throws Exception {
		assertConversion("if (c && ba && hello)", "if (ba && c && hello)", 7,
				1, SwapOperandsCompletionProposal.class);
	}

	public void testSwapOperands4() throws Exception {
		assertConversion("if (c && (ba && hello))", "if ((ba && hello) && c)",
				7, 1, SwapOperandsCompletionProposal.class);
	}

	public void testSwapOperands5() throws Exception {
		assertConversion("def r = ba == c.q.q.q.q == ddd",
				"def r = ddd == ba == c.q.q.q.q", 25, 1,
				SwapOperandsCompletionProposal.class);
	}

	public void testSwapOperands6() throws Exception {
		assertConversion("def r = ba == c.q.q.q.q == ddd",
				"def r = c.q.q.q.q == ba == ddd", 12, 1,
				SwapOperandsCompletionProposal.class);
	}
   
	public void testSwapOperands7() throws Exception {
	    assertConversion("v  && g && a",
	            "g  && v && a", "&&",
	            SwapOperandsCompletionProposal.class);
	}
	
	public void testSwapOperands8() throws Exception {
	    assertConversion("g  || a && v",
	            "g  || v && a", "&&",
	            SwapOperandsCompletionProposal.class);
	}
	
    private void assertConversion(String original, String expected, String searchFor, Class<? extends AbstractGroovyCompletionProposal> proposalClass) throws Exception, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        int start = searchFor == null ? 0 : original.indexOf(searchFor);
        int length = searchFor == null ? 0 : searchFor.length();
        assertConversion(original, expected, start, length, proposalClass);
    }
    private void assertConversion(String original, String expected, int offset, int length, Class<? extends AbstractGroovyCompletionProposal> proposalClass) throws Exception, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        ICompilationUnit unit = testProject.createUnit("", "QuickFix.groovy", original);
        
        IInvocationContext context = new AssistContext(unit, offset, length);
        AbstractGroovyCompletionProposal proposal = proposalClass.getConstructor(IInvocationContext.class).newInstance(context);
        assertTrue("Expecting that proposals exist for '" + proposal.getDisplayString() + "'", proposal.hasProposals());
        IDocument document = new Document(String.valueOf(((CompilationUnit) unit).getContents()));
        proposal.apply(document);
        
        assertEquals("Invalid application of quick assist", expected, document.get());
    }
}
