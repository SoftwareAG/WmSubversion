<?xml version='1.0'?>
<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Transitional//EN'
                      '/local/share/xml/XHTML/dtds/xhtml1-transitional.dtd'>

%invoke wm.svn.admin:getUiProperties%%endinvoke%

<html>
    <head>
        <title>%value uiProperties/productName%</title>
        <meta http-equiv='Content-Type' content='text/html; charset=UTF-8'></meta>
        <meta http-equiv="Pragma" content="no-cache"></meta>
        <meta http-equiv="Expires" content="-1"></meta>
        <link rel="stylesheet" type="text/css" href="../../WmRoot/webMethods.css"></link>
        <link rel="stylesheet" type="text/css" href="../svn.css"></link>
    </head>

    <body>
    
        %invoke wm.svn.admin:getStartupErrors%
            %scope startupErrors%
                <!-- Show startup errors, if any -->
                <table width=100%>
                    <tr>
                        <td id="message">
                            <b>Errors occured during package startup.</b>
                        </td>
                    </tr>
                    %loop -struct%
                        %scope #$key%
                            <tr class="message">
                                <td id="message">
                                    <b><a href=/WmSubversion/error/showErrorDetail.dsp?errorMessage=%value -urlencode $key%>%value message%</a></b>
                                    <a href=/invoke/wm.svn.error/getErrorDetail?errorMessage=%value -urlencode $key%>[alternate view]</a>
                                    <br>
                                    %ifvar reason%
                                        (caused by %value exceptionClass%: %value reason%)
                                    %endif%
                                </td>
                            </tr>
                        %endscope%
                    %endloop%
                </table>
                <!-- End startup errors -->
            %endscope%
        %endinvoke%
        
        %invoke wm.svn.admin:getConfiguration%
            %invoke wm.svn.admin.statistic:getStatistics%
                <table width=100%>
                
                    <tr>
                        <td class="menusection-Adapters" colspan=3>
                            %value uiProperties/button.name% &gt; %value uiProperties/tabs.1.name%
                        </td>
                    </tr>
            
                    <tr>
                        <td valign="top" width=50%>
                            <table class="table2" width=100%>
                                <tr>
                                    <td class="heading" colspan=3>
                                        General
                                    </td>
                                </tr>
                                
                                <tr>
                                    <td class="oddcol" nowrap="nowrap">
                                        %value uiProperties/button.name% Start Date
                                    </td>
                                    <td class="oddrowdata" colspan=2>
                                        %value statistics/packageLoadDate%
                                    </td>
                                </tr>
                                
                                <tr>
                                    <td class="heading" colspan=3>
                                        Logs
                                    </td>
                                </tr>

                                <tr>
                                    <td class="oddcol" nowrap="nowrap">
                                        %value uiProperties/button.name% Log
                                    </td>
                                    <td class="oddrowdata" colspan=2>
                                        <!-- this doesn't seem to work on all browsers -->
                                        <a href=/invoke/wm.svn.log/readLogFile?logId=WmSubversion>view</a>
                                    </td>
                                </tr>
                                
                                <tr>
                                    <td class="oddcol" nowrap="nowrap">
                                        %value uiProperties/button.name% Log Level
                                    </td>
                                    <td class="oddrowdata" colspan=1>
                                        %ifvar configuration/logLevel%
                                            %value configuration/logLevel%
                                        %else%
                                            Use server log level
                                        %endif%
                                    </td>
                                    <td class="oddrowdata">
                                        <a href=editLogLevel.dsp?logId=WmSubversion>change</a>
                                    </td>
                                </tr>
                                
                                <tr>
                                    <td class="oddcol" nowrap="nowrap">
                                        %value uiProperties/button.name% Errors
                                    </td>
                                    <td class="oddrowdata" colspan=2>
                                        <a href=/WmSubversion/error/showErrors.dsp>view</a>
                                    </td>
                                </tr>
                                
                                <tr>
                                    <td class="oddcol" nowrap="nowrap">
                                        %value uiProperties/button.name% Envelope Log
                                    </td>
                                    <td class="oddrowdata" colspan=2>
                                        <a href=envelopeLogging.dsp>view</a>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    
                        <td valign="top" width=50%>
                            <table width=100%>
                                <tr>
                                    <td class="heading" colspan=2>
                                        Test
                                    </td>
                                </tr>
                                <tr>
                                    <td class="oddcol" nowrap="nowrap" colspan=2>
                                        <a href=../test/chooseTest.dsp>Test Connection</a>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="oddcol" nowrap="nowrap" colspan=2>
                                        <a href=../test/submitEnvelopeToService.dsp>Test Send/Receive</a>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="oddcol" nowrap="nowrap" colspan=2>
                                        <a href=../test/parseStringTest.dsp>Validate Document</a>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
                <!-- end of top table -->

                %include envelopeStatisticsTable.dsp%

            %onerror%
                <!--
                %loop -struct%
                    %value $key%: %value%
                %endloop%
                -->
                %include ../../../WmSubversion/pub/error/onerrorTable.dsp%
            %endinvoke%
        
        %onerror%
            <!--
            %loop -struct%
                %value $key%: %value%
            %endloop%
            -->
            %include ../../../WmSubversion/pub/error/onerrorTable.dsp%
        %endinvoke%
    </body>
</html>
