<%= dojoInclude(["dojo.event.*",
                 "dojo.collections.Store",
                 "dojo.widget.ContentPane",
                 "dojo.widget.TabContainer",
                 "dojo.widget.FilteringTable"]) %>
<link rel=stylesheet href="/hqu/public/hqu.css" type="text/css">

<script type="text/javascript">
function getSystemStats() {
  dojo.io.bind({
    url: '<%= urlFor(action:"getSystemStats") %>',
    method: "post",
    mimetype: "text/json-comment-filtered",
    load: function(type, data, evt) {
      dojo.byId('userCPU').innerHTML = data.sysUserCpu;
      dojo.byId('userCPUBar').style.width = data.sysUserCpu;
      dojo.byId('sysCPU').innerHTML  = data.sysSysCpu;
      dojo.byId('sysCPUBar').style.width = data.sysSysCpu;
      dojo.byId('niceCPU').innerHTML = data.sysNiceCpu;
      dojo.byId('idleCPU').innerHTML = data.sysIdleCpu;
      dojo.byId('waitCPU').innerHTML = data.sysWaitCpu;

      dojo.byId('loadAvg1').innerHTML  = data.loadAvg1;
      dojo.byId('loadAvg5').innerHTML  = data.loadAvg5;
      dojo.byId('loadAvg15').innerHTML = data.loadAvg15;

      dojo.byId('totalMem').innerHTML = data.totalMem;
      dojo.byId('usedMem').innerHTML  = data.usedMem;
      dojo.byId('usedMemBar').style.width = data.percMem;
      dojo.byId('freeMem').innerHTML  = data.freeMem;

      dojo.byId('totalSwap').innerHTML = data.totalSwap;
      dojo.byId('usedSwap').innerHTML  = data.usedSwap;
      dojo.byId('usedSwapBar').style.width = data.percSwap;
      dojo.byId('freeSwap').innerHTML  = data.freeSwap;
      
      dojo.byId('pid').innerHTML         = data.pid;
      dojo.byId('procStartTime').innerHTML = data.procStartTime;
      dojo.byId('procOpenFds').innerHTML = data.procOpenFds;
      
      dojo.byId('procMemSize').innerHTML  = data.procMemSize;
      dojo.byId('procMemRes').innerHTML   = data.procMemRes;
      dojo.byId('procMemShare').innerHTML = data.procMemShare;

      dojo.byId('procCpu').innerHTML = data.procCpu;
      dojo.byId('procCpuBar').style.width = data.procCpu;
      
      dojo.byId('sysPercCpu').innerHTML = data.sysPercCpu;
      dojo.byId('sysPercCpuBar').style.width = data.sysPercCpu;
      dojo.byId('percMem').innerHTML    = data.percMem;
      dojo.byId('percMemBar').style.width = data.percMem;
      dojo.byId('percSwap').innerHTML   = data.percSwap;
      dojo.byId('percSwapBar').style.width = data.percSwap;
      
      
      dojo.byId('jvmTotalMem').innerHTML  = data.jvmTotalMem
      dojo.byId('jvmFreeMem').innerHTML   = data.jvmFreeMem
      dojo.byId('jvmMaxMem').innerHTML    = data.jvmMaxMem
      dojo.byId('jvmPercMem').innerHTML   = data.jvmPercMem
    },
  });
  setTimeout("getSystemStats()", 1000);
}

function selectDiag(d) {
  dojo.io.bind({
    url: '<%= urlFor(action:"getDiag") %>' + '?diag=' + d,
    method: "post",
    mimetype: "text/json-comment-filtered",
    load: function(type, data, evt) {
      dojo.byId('diagData').innerHTML = data.diagData;
    },
  });
}

getSystemStats();
</script>

<div id="metrics">
<div class="metricGroupBlock">

  <div id="systemLoad" class="metricGroup">
    <div class="metricCatLabel">${l.sysLoad}</div>
    <table class="metricTable">
    <tr class="metricRow">
      <td>${l.oneMin}:</td><td><span class="metricValue" id="loadAvg1"></span></td>
    </tr>
    <tr class="metricRow">
      <td>${l.fiveMin}:</td><td><span class="metricValue" id="loadAvg5"></span></td>
    </tr>
    <tr>
      <td >${l.fifteenMin}:</td><td><span class="metricValue" id="loadAvg15"></span></td>
    </tr class="metricRow">
    </table>
  </div>
  
  <div id="processorStats" class="metricGroup">
    <div class="metricCatLabel">${l.sysProcStats}</div>
    <table class="metricTable">
    <tr class="metricRow">
      <td>${l.user}:</td><td><div class="barContainer"><div id="userCPUBar" class="bar"><span class="metricValue" id="userCPU"></span>%</div></div></td>
    </tr>
    <tr class="metricRow">
      <td>${l.system}:</td><td><div class="barContainer"><div id="sysCPUBar" class="bar"><span class="metricValue" id="sysCPU"></span>%</div></div></td>
    </tr>
    <tr class="metricRow">
      <td>${l.nice}:</td><td><span class="metricValue" id="niceCPU"></span>%</td>
    </tr>
    <tr class="metricRow">
      <td>${l.idle}:</td><td><span class="metricValue" id="idleCPU"></span>%</td>
    </tr>
    <tr class="metricRow">  
      <td>${l.wait}:</td><td><span class="metricValue" id="waitCPU"></span>%</td>
    </tr>
    </table>
  </div>
</div>

<div class="metricGroupBlock"> 
  <div id="memStats" class="metricGroup">
    <div class="metricCatLabel">${l.sysMemStats}</div>
    <table class="metricTable">
    <tr class="metricRow">
      <td>${l.total}:</td><td><span class="metricValue" id="totalMem"></span></td>
    </tr>
    <tr class="metricRow">
      <td>${l.used}:</td><td><div class="barContainer"><div id="usedMemBar" class="bar"><span class="metricValue" id="usedMem"></span></div></div></td>
    </tr>
    <tr class="metricRow">
      <td>${l.free}:</td><td><span class="metricValue" id="freeMem"></span></td>
    </tr>
    </table>
  </div>

  <div id="swapStats" class="metricGroup">
    <div class="metricCatLabel">${l.sysSwapStats}</div>
    <table class="metricTable">
    <tr class="metricRow">
      <td>${l.total}:</td><td><span class="metricValue" id="totalSwap"></span></td>
    </tr>
    <tr class="metricRow">
      <td>${l.used}:</td><td><div class="barContainer"><div id="usedSwapBar" class="bar"><span class="metricValue" id="usedSwap"></span></div></div></td>
     </tr>
    <tr class="metricRow">
      <td>${l.free}:</td><td><span class="metricValue" id="freeSwap"></span></td>
    </tr>
    </table>
  </div>
</div>

<div class="metricGroupBlock">
  <div id="processInfo" class="metricGroup">
    <div class="metricCatLabel">${l.procInfo}</div>
    <table class="metricTable">
    <tr class="metricRow">
      <td>${l.pid}:</td><td><span id="pid"></span></td>
    </tr>
    <tr class="metricRow">
      <td>${l.procOpenFds}:</td><td><span id="procOpenFds"></span></td>
    </tr>
    <tr class="metricRow">
      <td>${l.procStartTime}:</td><td><span id="procStartTime"></span></td>
    </tr>
    <tr class="metricRow">
      <td>${l.procMemSize}:</td><td><span id="procMemSize"></span></td>
    </tr>
    <tr class="metricRow">
      <td>${l.procMemRes}:</td><td><span id="procMemRes"></span></td>
    </tr>
    <tr class="metricRow">
      <td>${l.procMemShare}:</td><td><span id="procMemShare"></span></td>
    </tr>
    <tr class="metricRow">
      <td>${l.procCpu}:</td><td><div class="barContainer"><div id="procCpuBar" class="bar"><span id="procCpu"></span>%</div></div></td>
    </tr>
    </table>
  </div>
  
  <div id="mailLinks" class="metricGroup">
    <div class="metricCatLabel">Actions</div>
      <blockquote>
        <span>${linkTo('Print', [action:'printReport'])}</span>
      </blockquote>
  </div>
</div>

<div class="metricGroupBlock">
  <div id="jvmInfo" class="metricGroup">
  	<div class="metricCatLabel">${l.jvm}</div>
    <table class="metricTable">
    <tr class="metricRow">
      <td>${l.jvmPercMem}:</td><td><span id="jvmPercMem"></span></td>
    </tr>
    <tr class="metricRow">
      <td>${l.jvmFreeMem}:</td><td><span id="jvmFreeMem"></span></td>
    </tr>
    <tr class="metricRow">
      <td>${l.jvmTotalMem}:</td><td><span id="jvmTotalMem"></span></td>
    </tr>
    <tr class="metricRow">
      <td>${l.jvmMaxMem}:</td><td><span id="jvmMaxMem"></span></td>
    </tr>
    </table>
  </div>
    
  <div id="percentages" class="metricGroup">
    <div class="metricCatLabel">${l.perc}</div>
    <table class="metricTable">
    <tr class="metricRow">
      <td>${l.cpuUsed}:</td><td><div class="barContainer"><div id="sysPercCpuBar" class="bar"><span id="sysPercCpu"></span>%</div></div></td>
    </tr>
    <tr class="metricRow">
      <td>${l.memUsed}:</td><td><div class="barContainer"><div id="percMemBar" class="bar"><span id="percMem"></span>%</div></div></td>
    </tr>
    <tr class="metricRow">
      <td>${l.swapUsed}:</td><td><div class="barContainer"><div id="percSwapBar" class="bar"><span id="percSwap"></span>%</div></div></td>
    </tr>
    </table>
  </div>
  
</div>
</div>
<div id="fullBody" style="clear:both">
  <div dojoType="TabContainer" id="bodyTabContainer" style="width: 100%; height:500px;">
    <div dojoType="ContentPane" label="${l.diagnostics}">
      <select id="diagSelect" onchange='selectDiag(options[selectedIndex].value)'>
      <% for (d in diags) { %>
        <option value='${d.shortName}'>${h d.name}</option>
      <% } %>
      </select>
      <pre>
        <div id="diagData">
        </div>
      </pre>
    </div>  

    <div dojoType="ContentPane" label="Cache">
      <%= dojoTable(id:'cacheTable', title:l.cache,
                    refresh:60, url:urlFor(action:'cacheData'),
                    schema:cacheSchema, numRows:500, pageControls:false) %>
    </div>  

    <div dojoType="ContentPane" label="Load">
      ${l.metricsPerMinute}: ${metricsPerMinute}
    </div>  

  </div>
</div>
