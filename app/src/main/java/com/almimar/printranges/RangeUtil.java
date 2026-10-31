package com.almimar.printranges;
import java.util.*;
public final class RangeUtil {
 static List<Integer> pages(String input,String fallback,int max,boolean even,boolean reverse){
  String s=input.trim().isEmpty()?fallback.trim():input.trim();
  s=s.replace('،',',').replace('؛',',').replaceAll("\\s+","");
  ArrayList<Integer> out=new ArrayList<>();
  if(s.isEmpty()){for(int i=1;i<=max;i++) if((i%2==0)==even)out.add(i);}
  else {
   TreeSet<Integer> set=new TreeSet<>();
   for(String tok:s.split(",",-1)){
    String[] parts=tok.split("-",-1);
    if(parts.length<1||parts.length>2)throw new IllegalArgumentException("نطاق غير صحيح: "+tok);
    int start=parse(parts[0]); int end=parts.length==2?parse(parts[1]):start;
    if(start<1||end>max||start>end)throw new IllegalArgumentException("الصفحات يجب أن تكون بين 1 و"+max);
    for(int i=start;i<=end;i++)if((i%2==0)==even)set.add(i);
   }
   out.addAll(set);
  }
  if(reverse)Collections.reverse(out);
  return out;
 }
 static int parse(String s){try{return Integer.parseInt(s);}catch(Exception e){throw new IllegalArgumentException("رقم غير صالح: "+s);}}
 static List<Integer> both(List<Integer> odd,List<Integer> even){TreeSet<Integer> set=new TreeSet<>(odd);set.addAll(even);return new ArrayList<>(set);}
}