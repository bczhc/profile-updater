#!/usr/bin/env wolframscript
(* ::Package:: *)

If[Length@$ScriptCommandLine==0,Exit[],0]
argv=Rest@$ScriptCommandLine;
argc=Length@argv;
If[argc!=2,(Print["Usage: command <commit-author-time-list-file> <output-file>"];Exit[]),0]

intItpr=Interpreter["Integer"];

timezoneOffset[time_]:=(
timezoneStr=StringSplit[time," "][[3]];
sign=If[StringPart[timezoneStr,1]=="+",1,-1];
(intItpr@StringTake[timezoneStr,{2,3}]
+intItpr@StringTake[timezoneStr,{4,5}]/60)*sign//N
);
parseDate[dateStr_]:=DateObject[
(split=StringSplit[dateStr];split[[1]]<>" "<>split[[2]]),
TimeZone->timezoneOffset[dateStr]
];

aTimesRead=Import[argv[[1]],"Text"];
aTimes=(parseDate[#])&/@StringSplit[aTimesRead,"\n"];
tObjs=(TimeObject[DateValue[#,{"Hour","Minute","Second"}]])&/@aTimes;
ts=TimeSeries[( 
dv=DateValue[#,{"Hour","Minute","Second"}];
dv[[1]]+dv[[2]]/60+dv[[3]]/60^2
)&/@tObjs,{aTimes}]
plot=DateListPlot[ts,
Joined->False,
ScalingFunctions->"Reverse",
AspectRatio->1/2,
FrameLabel->{"Date","Time"},
ImageSize->Large,
FrameTicks->{{Table[{x,StringRepeat["0",2-StringLength[ToString@x]]<>ToString@x<>":00"},{x,0,24,2}],None},{Automatic,None}},
PlotRange->{Automatic,{-0.2,24.2}},
PlotLabel->"Commits"
]
Export[argv[[2]],plot]
