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
commitHours=( 
date=parseDate[#];
DateValue[date,"Hour"]
)&/@StringSplit[aTimesRead,"\n"];
hours=Table[h,{h,0,23}];
hoursCounts=(Count[commitHours,#])&/@hours;
chart=BarChart[hoursCounts,
LabelingFunction -> (Placed[Style[ToString@#, "Label", 8], Above] &),
ChartLabels->Placed[
StringRepeat["0",2-StringLength[ToString@#]]<>ToString@#&/@hours,
Axis],
AspectRatio->1/2,
AxesLabel->{h,c},
PlotLabel->"Commits per day hour"
];
Export[argv[[2]],chart]
