package example;

import java.io.IOException;
import java.io.StringReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;

import edu.gmu.swe.datadep.HeapWalker;
import edu.gmu.swe.datadep.StaticFieldDependency;
import example.data.DummyTest;

public class MapIT {

	JUnitCore junitCore = new JUnitCore();

	void executeTest(Class<?> testClass, String methodName) {
		System.out.println("ExecuteTest() " + testClass.getName() + "." + methodName);
		if (junitCore.run(Request.method(testClass, methodName)).getFailureCount() > 0)
			Assert.fail("Test " + testClass.getName() + "." + methodName + " FAILED ");
	}

	@BeforeClass
	public static void setupWhitelist() {
		HeapWalker.resetAllState();
		HeapWalker.clearWhitelist();
		String[] whiteList = new String[] { //
				"example.data", //
		};

		for (String wl : whiteList)
			HeapWalker.addToWhitelist(wl);
		System.out.println("MapIT.setupWhitelist() Whitelist loaded ");

	}

	// TODO Probably a matchers of some sort is already available...
	static void has(Collection<Entry<String, String>> depsData, String testName, String depName) {
		for (Entry<String, String> d : depsData) {
			if (d.getValue().equals(testName) && d.getKey().equals(depName)) {
				return;
			}
		}
		// TODO Potential Alternatives - help debugging
		Assert.fail("Missing dep: " + depName + " -- " + testName + " in " + depsData);
	}

	/**
	 * Fails if we find the specified dependency
	 * 
	 * @param depsData
	 * @param testName
	 * @param depName
	 */
	static void hasNot(Collection<Entry<String, String>> depsData, String testName, String depName) {
		for (Entry<String, String> d : depsData) {
			if (d.getValue().equals(testName) && d.getKey().equals(depName)) {
				Assert.fail("Found unexpected: " + depName + " -- " + testName + " in " + depsData);
			}
		}
	}

	// Copied from java.lang.reflect.Field
	static String getTypeName(Class<?> type) {
		if (type.isArray()) {
			try {
				Class<?> cl = type;
				int dimensions = 0;
				while (cl.isArray()) {
					dimensions++;
					cl = cl.getComponentType();
				}
				StringBuffer sb = new StringBuffer();
				sb.append(cl.getName());
				for (int i = 0; i < dimensions; i++) {
					sb.append("[]");
				}
				return sb.toString();
			} catch (Throwable e) {
				/* FALLTHRU */ }
		}
		return type.getName();
	}

	// Return the string/xml of the value for this sf
	Collection<Entry<String, String>> extractDataStaticFieldDepValue(Class dataSourceTestClass,
			LinkedList<StaticFieldDependency> deps) {

		Collection<Entry<String, String>> values = new ArrayList<Entry<String, String>>();
		// There might be more ....
		for (StaticFieldDependency sf : deps) {
			if (sf.field.getType().isAssignableFrom(dataSourceTestClass)) {
				values.addAll(extractDepsData(sf));
			}
		}
		// Assert.fail("Cannot find value of data static field as dep");
		// return new ArrayList<Entry<String, String>>();
		return values;
	}

	Collection<Entry<String, String>> extractAllDepValues(LinkedList<StaticFieldDependency> deps) {

		Collection<Entry<String, String>> values = new ArrayList<Entry<String, String>>();
		// There might be more ....
		for (StaticFieldDependency sf : deps) {
			values.addAll(extractDepsData(sf));
		}
		return values;
	}

	Collection<Entry<String, String>> extractDepsData(StaticFieldDependency sf) {
		List<Entry<String, String>> deps = new ArrayList<Entry<String, String>>();
		// Extract root dep
		deps.add(new AbstractMap.SimpleEntry<String, String>(
				(getTypeName(sf.field.getDeclaringClass()) + "." + sf.field.getName()),
				HeapWalker.testNumToTestClass.get(sf.depGen) + "." + HeapWalker.testNumToMethod.get(sf.depGen)));

		// Extract deps on values
		String xmlValue = sf.value;

		if (xmlValue == null || xmlValue.trim().equals("")) {
			System.out.println("AbstractCrystalIT.extractDepsData() Empry xmlValue for static field " + sf);
			// Can we say the it is the dep here ?
			return deps;
		}
		SAXBuilder builder = new SAXBuilder();
		try {

			Document document = (Document) builder.build(new StringReader(xmlValue));

			// Select only elements with the dependsOn attribute
			String query = "//*[@dependsOn]";
			XPathExpression<Element> xpe = XPathFactory.instance().compile(query, Filters.element());
			for (Element urle : xpe.evaluate(document)) {

				// String[] depAttrs =
				// urle.getAttribute("dependsOn").getValue().split(",");
				String depAttr = urle.getAttribute("dependsOn").getValue();

				Entry<String, String> dep = new AbstractMap.SimpleEntry<String, String>(urle.getName(), depAttr);
				deps.add(dep);
			}

		} catch (IOException io) {
			System.out.println(io.getMessage());
			Assert.fail(io.getMessage());
		} catch (JDOMException jdomex) {
			System.out.println(jdomex.getMessage());
			Assert.fail(jdomex.getMessage());
		}

		return deps;
	}

	@Test
	public void testAdd() {
		LinkedList<StaticFieldDependency> deps;
		Collection<Entry<String, String>> depsData;

		//
		// TODO If something is declared by not initialized while something else
		// is initialize at null which one creates a conflict?
		//
		executeTest(DummyTest.class, "testSet");

		// This writes to several part
		deps = HeapWalker.walkAndFindDependencies("example.data.DummyTest", "testSet");
		System.out.println("MapIT.testAdd() DEPS " + deps);
		// depsData = extractAllDepValues(deps);
		// Assert.assertTrue(depsData.isEmpty());

		//
		executeTest(DummyTest.class, "testGet");
		deps = HeapWalker.walkAndFindDependencies("example.data.DummyTest", "testGet");
		System.out.println("MapIT.testAdd() DEPS " + deps);
		// depsData = extractAllDepValues(deps);
		// Assert.assertFalse(depsData.isEmpty());
	}

}
