<%@page pageEncoding="UTF-8" contentType="text/html; charset=UTF-8"%>
<h2>Query Syntax</h2>
<dl>
	<dt>Field</dt>
	<dd>
		You can search any field by typing the field name followed by a colon
		":" and then the term you are looking for. If you want to find
		documents which has "Fess" as the document title, you can enter:
		<pre>title:Fess</pre>
		The available fields are "url", "host", "site", "title", "content",
		"contentLength", "lastModified" and "mimetype", and they are
		customizable.
	</dd>
	<dt>Sort</dt>
	<dd>
		sort field sorts documents by a specified field name. The format is
		"sort:&lt;field&gt;.&lt;order&gt;", where &lt;order&gt; is asc or
		desc. If you want to find documents which has "Fess" and sort them in
		descending order, you can enter:
		<pre>Fess sort:contentLength.desc</pre>
		The available sort field are "created", "contentLength" and
		"lastModified", and they are customizable.
	</dd>
	<dt>AND</dt>
	<dd>
		AND operator is the default conjunction operator. You can omit it from
		a query. AND operator matches documents where both terms exist
		anywhere in the text of a single document.
		<pre>Fess AND Solr</pre>
	</dd>
	</dd>
	<dt>OR</dt>
	<dd>
		OR operator matches documents where any terms exist anywhere in the
		text of a single document.
		<pre>Fess OR Solr</pre>
	</dd>
	<dt>Wildcard</dt>
	<dd>
		single and multiple character wildcard searches within single terms
		are supported. "?" symbol is a single character wildcard search, and
		"*" is a multiple character wildcard search.
		<pre>Fess*</pre>
		or
		<pre>Fe?s</pre>
	</dd>
	<dt>Range</dt>
	<dd>
		Range Queries allow one to match documents whose field(s) values are
		between the lower and upper bound specified by the Range Query. Range
		Queries can be inclusive or exclusive of the upper and lower bounds.
		If you want to find documents whose contentLength fields have values
		between 1000 and 10000, inclusive, you can enter:
		<pre>contentLength:[1000 TO 10000]</pre>
		If you want to exclude the upper and lower bounds, use "{}".
	</dd>
	<dt>Boost</dt>
	<dd>
		To boost a term use the "^" symbol with a boost factor (a number) at
		the end of the term you are searching.
		<pre>Fess^100</pre>
	</dd>
	<dt>Fuzzy</dt>
	<dd>
		To do a fuzzy search use the "~" symbol at the end of a single word
		term. For example to search for a term similar in spelling to "Fess"
		use the fuzzy search:
		<pre>Fess~0.5</pre>
	</dd>
</dl>
<h2>Others</h2>
<dl>
	<dt>Add To Browser Search Engines</dt>
	<dd>
		Click <a href="#" onclick='window.external.AddSearchProvider("http://localhost:8080/fess/osdd");return false;'>here</a>
		 to add a search engine list for your browser.
	</dd>
</dl>

