#!/usr/bin/env wolframscript
(* ::Package:: *)

If[Length@$ScriptCommandLine==0,Exit[],0]
argv=Rest@$ScriptCommandLine;
argc=Length@argv;
If[argc!=2,(Print["Usage: command <commit-author-time-list-file> <output-file>"];Exit[]),0]

aTimestamps=Import[argv[[1]],"Text"];
intItpr=Interpreter["Integer"];
commitHours=( 
date=FromUnixTime[intItpr[#]];
DateValue[date,"Hour"]
)&/@StringSplit[aTimestamps,"\n"];
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
