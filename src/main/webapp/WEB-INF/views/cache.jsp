<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@page import="io.bdrc.iiif.core.EHServerCache"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<style>
table {
    border-collapse: collapse;
    border-spacing: 0;
    margin-left: 20px;    
    border: 1px solid #ddd;
}

td {
    text-align: left;
    vertical-align:top;
    padding: 16px;
}

th {
    padding-top: 12px;
    padding-bottom: 12px;
    text-align: center;
    background-color: #4e7F50;
    color: white;
}

tr:nth-child(even) {
    background-color: #f2f2f2
}
input[type=text], select {
    width: 100%;
    padding: 12px 20px;
    margin: 8px 0;
    display: inline-block;
    border: 1px solid #ccc;
    border-radius: 4px;
    box-sizing: border-box;
}

input[type=submit] {
    width: 80%;
    background-color: #4CAF50;
    color: white;
    padding: 14px 20px;
    margin: 8px 0;
    border: none;
    border-radius: 4px;
    cursor: pointer;
}

input[type=submit]:hover {
    background-color: #45a049;
}

</style>
<title>Cache and memory monitoring</title>
</head>
<body>
<h2>Cache and memory monitoring</h2>
<h3>DISK CACHE INFO/STATS</h3>
<c:forEach items="${EHServerCache.getDiskCachesNames()}" var="k">
  <c:set var="st" value="${EHServerCache.getCacheStatistics(k)}"/>
<table style="width: 60%;">
<tr><th colspan="2">Cache status and statistics for ${k}</th></tr>

<!-- tr><td><b>Cache status</b></td><td></td><td></td></tr>
<tr><td><b>Created Time</b></td><td></td><td>Cache region creation Time</td></tr>
<tr><td><b>Last Accessed Time</b></td><td></td><td>last time the cache was used</td></tr>
<tr><td><b>Objects</b></td><td></td><td>The total number of objects held by the cache</td></tr-->
<tr><td><b>Cache hits</b></td><td>${st.getCacheHits()}</td><td>How many hits occurred on the cache since its creation or the latest "clear"</td></tr>
<tr><td><b>Cache hits %</b></td><td>${st.getCacheHitPercentage()}</td><td>The percentage of hits compared to all gets since the cache creation or the latest "clear"</td></tr>
<tr><td><b>Misses</b></td><td>${st.getCacheMisses()}</td><td>How many misses occurred on the cache since its creation or the latest "clear"</td></tr>
<tr><td><b>Misses %</b></td><td>${st.getCacheMissPercentage()}</td><td>The percentage of misses compared to all gets since the cache creation or the latest "clear"</td></tr>
<tr><td><b>Gets</b></td><td>${st.getCacheGets()}</td><td>How many gets occurred on the cache since its creation or the latest "clear"</td></tr>
<tr><td><b>Puts</b></td><td>${st.getCachePuts()}</td><td>How many puts occurred on the cache since its creation or the latest "clear"</td></tr>
<tr><td><b>Removals</b></td><td>${st.getCacheRemovals()}</td><td>How many removals occurred on the cache since its creation or the latest "clear"</td></tr>
<tr><td><b>Evictions</b></td><td>${st.getCacheEvictions()}</td><td>How many evictions occurred on the cache since its creation or the latest "clear"</td></tr>
<tr><td><b>Expirations</b></td><td>${st.getCacheExpirations()}</td><td>How many expirations occurred on the cache since its creation or the latest "clear"</td></tr>
</table>
<br>
<table style="width: 30%;">
<tr><th colspan="2">Heap/Disk Info for ${k}</th></tr>
<c:forEach items="${st.getTierStatistics().keySet()}" var="k">
    <c:set var="ts" value="${st.getTierStatistics().get(k)}"/>
    <c:forEach items="${ts.getKnownStatistics().keySet()}" var="key">
    <tr><td><b>${key}</b></td><td>${ts.getKnownStatistics().get(key).value()}</td></tr>
    </c:forEach>
</c:forEach>
</table>
<br><br>
</c:forEach>
<h3>MEMORY CACHE INFO/STATS</h3>
<c:forEach items="${EHServerCache.getMemoryCachesNames()}" var="k">
  <c:set var="st" value="${EHServerCache.getCacheStatistics(k)}"/>
<table style="width: 60%;">
<tr><th colspan="2">Cache status and statistics for ${k}</th></tr>
<!-- tr><td><b>Cache status</b></td><td></td><td></td></tr>
<tr><td><b>Created Time</b></td><td></td><td>Cache region creation Time</td></tr>
<tr><td><b>Last Accessed Time</b></td><td></td><td>last time the cache was used</td></tr>
<tr><td><b>Objects</b></td><td></td><td>The total number of objects held by the cache</td></tr-->
<tr><td><b>Cache hits</b></td><td>${st.getCacheHits()}</td><td>How many hits occurred on the cache since its creation or the latest "clear"</td></tr>
<tr><td><b>Cache hits %</b></td><td>${st.getCacheHitPercentage()}</td><td>The percentage of hits compared to all gets since the cache creation or the latest "clear"</td></tr>
<tr><td><b>Misses</b></td><td>${st.getCacheMisses()}</td><td>How many misses occurred on the cache since its creation or the latest "clear"</td></tr>
<tr><td><b>Misses %</b></td><td>${st.getCacheMissPercentage()}</td><td>The percentage of misses compared to all gets since the cache creation or the latest "clear"</td></tr>
<tr><td><b>Gets</b></td><td>${st.getCacheGets()}</td><td>How many gets occurred on the cache since its creation or the latest "clear"</td></tr>
<tr><td><b>Puts</b></td><td>${st.getCachePuts()}</td><td>How many puts occurred on the cache since its creation or the latest "clear"</td></tr>
<tr><td><b>Removals</b></td><td>${st.getCacheRemovals()}</td><td>How many removals occurred on the cache since its creation or the latest "clear"</td></tr>
<tr><td><b>Evictions</b></td><td>${st.getCacheEvictions()}</td><td>How many evictions occurred on the cache since its creation or the latest "clear"</td></tr>
<tr><td><b>Expirations</b></td><td>${st.getCacheExpirations()}</td><td>How many expirations occurred on the cache since its creation or the latest "clear"</td></tr>
</table>
</c:forEach>
<br><br>
<h3>IIIFSERV MEMORY WATCH</h3>
<table style="width: 40%;">
<tr><th colspan="2">Heap memory usage</th><th colspan="2">Non-Heap memory usage</th></tr>
<tr><td style="text-align: right;">Committed</td><td style="text-align: right;">${model.getHeapCommitted()}</td>
<td style="text-align: right;">Committed</td><td style="text-align: right;">${model.getNonHeapCommitted()}</td></tr>
<tr><td style="text-align: right;">Init. requested</td><td style="text-align: right;">${model.getHeapInit()}</td>
<td style="text-align: right;">Init. requested</td><td style="text-align: right;">${model.getNonHeapInit()}</td></tr>
<tr><td style="text-align: right;">Max</td><td style="text-align: right;">${model.getHeapMax()}</td>
<td style="text-align: right;">Max</td><td style="text-align: right;">${model.getNonHeapMax()}</td></tr>
<tr><td style="text-align: right;">Used</td><td style="text-align: right;">${model.getHeapUsed()}</td>
<td style="text-align: right;">Used</td><td style="text-align: right;">${model.getNonHeapUsed()}</td></tr>
</table>
<br>
<h3>IIIFSERV MEMORY POOL DETAILS</h3>
<table style="width: 60%;">
<tr><th>Memory Pool</th><th>Init</th><th>Committed</th><th>Max</th><th>Used</th></tr>
<tr>
<td>Code Cache</td>
<td>${model.format(model.getCodeMemoryUsage().getInit())}</td>
<td>${model.format(model.getCodeMemoryUsage().getCommitted())}</td>
<td>${model.format(model.getCodeMemoryUsage().getMax())}</td>
<td>${model.format(model.getCodeMemoryUsage().getUsed())}</td>
</tr>
<tr>
<td>Meta Space</td>
<td>${model.format(model.getMetaMemoryUsage().getInit())}</td>
<td>${model.format(model.getMetaMemoryUsage().getCommitted())}</td>
<td>${model.format(model.getMetaMemoryUsage().getMax())}</td>
<td>${model.format(model.getMetaMemoryUsage().getUsed())}</td>
</tr>
<tr>
<td>Compressed Class Space</td>
<td>${model.format(model.getCompressedMemoryUsage().getInit())}</td>
<td>${model.format(model.getCompressedMemoryUsage().getCommitted())}</td>
<td>${model.format(model.getCompressedMemoryUsage().getMax())}</td>
<td>${model.format(model.getCompressedMemoryUsage().getUsed())}</td>
</tr>
<tr>
<td>PS Eden Space</td>
<td>${model.format(model.getEdenMemoryUsage().getInit())}</td>
<td>${model.format(model.getEdenMemoryUsage().getCommitted())}</td>
<td>${model.format(model.getEdenMemoryUsage().getMax())}</td>
<td>${model.format(model.getEdenMemoryUsage().getUsed())}</td>
</tr>
<tr>
<td>PS Survivor Space</td>
<td>${model.format(model.getSurvivorMemoryUsage().getInit())}</td>
<td>${model.format(model.getSurvivorMemoryUsage().getCommitted())}</td>
<td>${model.format(model.getSurvivorMemoryUsage().getMax())}</td>
<td>${model.format(model.getSurvivorMemoryUsage().getUsed())}</td>
</tr>
<tr>
<td>PS Old Gen</td>
<td>${model.format(model.getOldMemoryUsage().getInit())}</td>
<td>${model.format(model.getOldMemoryUsage().getCommitted())}</td>
<td>${model.format(model.getOldMemoryUsage().getMax())}</td>
<td>${model.format(model.getOldMemoryUsage().getUsed())}</td>
</tr>
</table>
</body>
</html>