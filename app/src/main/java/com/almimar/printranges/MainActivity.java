package com.almimar.printranges;
import android.app.*;
import android.os.*;
import android.net.Uri;
import android.content.*;
import android.graphics.Color;
import android.print.*;
import android.widget.*;
import android.view.*;
import androidx.core.content.FileProvider;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import java.io.*;
import java.util.*;
public class MainActivity extends Activity {
 EditText general,odd,even; CheckBox reverseOdd,reverseEven; TextView info;
 Uri original; File source,produced; int count;
 final int PICK=17,SAVE=18;
 LinearLayout layout;
 int blue=Color.rgb(25,64,128);
 TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(blue);t.setPadding(8,10,8,10);return t;}
 EditText field(String label,String hint){
 layout.addView(text(label,17)); EditText t=new EditText(this);t.setSingleLine(true);t.setHint(hint);t.setTextDirection(View.TEXT_DIRECTION_LTR);layout.addView(t);return t;
 }
 void button(String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);layout.addView(b);b.setOnClickListener(v->action.run());}
 @Override public void onCreate(Bundle s){super.onCreate(s);PDFBoxResourceLoader.init(this);
 ScrollView scroll=new ScrollView(this);layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(20,20,20,25);layout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);scroll.addView(layout);setContentView(scroll);
 layout.addView(text("المعمار | نطاقات الطباعة",23));
 layout.addView(text("نطاق مستقل للفردي ونطاق مستقل للزوجي",16));
 button("اختيار ملف PDF",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");startActivityForResult(i,PICK);});
 info=text("لم تختر ملفًا",15);layout.addView(info);
 general=field("النطاق العام (اختياري)","1-50");
 odd=field("النطاق الفردي المستقل","5-27 أو 5,9,13");
 even=field("النطاق الزوجي المستقل","2-18 أو 4,8,12");
 layout.addView(text("النطاقان المستقلان لا يتقيدان بالنطاق العام. اترك النطاق مستقلًا فارغًا لاستخدام النطاق العام.",13));
 reverseOdd=new CheckBox(this);reverseOdd.setText("عكس ترتيب الفردي");layout.addView(reverseOdd);
 reverseEven=new CheckBox(this);reverseEven.setText("عكس ترتيب الزوجي");layout.addView(reverseEven);
 button("طباعة الفردي",()->operate(0,0));
 button("طباعة الزوجي",()->operate(1,0));
 button("طباعة الفردي والزوجي",()->operate(2,0));
 button("حفظ PDF الفردي",()->operate(0,1));
 button("حفظ PDF الزوجي",()->operate(1,1));
 button("حفظ PDF موحد",()->operate(2,1));
 button("مشاركة الفردي مع PrintHand",()->operate(0,2));
 button("مشاركة الزوجي مع PrintHand",()->operate(1,2));
 Intent incoming=getIntent();if(Intent.ACTION_VIEW.equals(incoming.getAction())&&incoming.getData()!=null)load(incoming.getData());
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;
 if(request==PICK)load(data.getData());
 if(request==SAVE&&produced!=null){try(InputStream in=new FileInputStream(produced);OutputStream out=getContentResolver().openOutputStream(data.getData())){copy(in,out);info.setText("تم حفظ PDF");}catch(Exception e){info.setText(e.getMessage());}}
 }
 static void copy(InputStream in,OutputStream out)throws IOException{byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);}
 void load(Uri uri){original=uri;info.setText("جارٍ فتح PDF ...");
 new Thread(()->{try{
 File f=new File(getCacheDir(),"source.pdf");try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(f)){copy(in,out);}
 int c;try(PDDocument doc=PDDocument.load(f)){c=doc.getNumberOfPages();}
 runOnUiThread(()->{source=f;count=c;info.setText("المستند يحتوي "+c+" صفحة");});
 }catch(Exception e){fail(e);}}).start();
 }
 void fail(Exception e){runOnUiThread(()->info.setText("خطأ: "+e.getMessage()));}
 void operate(int mode,int action){if(source==null){info.setText("اختر PDF أولًا");return;}
 try{
 String common=general.getText().toString();
 List<Integer> o=RangeUtil.pages(odd.getText().toString(),common,count,false,reverseOdd.isChecked());
 List<Integer> e=RangeUtil.pages(even.getText().toString(),common,count,true,reverseEven.isChecked());
 List<Integer> chosen=mode==0?o:mode==1?e:RangeUtil.both(o,e);
 if(chosen.isEmpty()){info.setText("لا توجد صفحات مطابقة للنطاق");return;}
 info.setText("تجهيز "+chosen.size()+" صفحة ...");
 new Thread(()->{try{
 File dir=new File(getCacheDir(),"print_exports");dir.mkdirs();
 File pdf=File.createTempFile("almimar_pages_",".pdf",dir);
 try(PDDocument input=PDDocument.load(source);PDDocument output=new PDDocument()){
 for(int page:chosen)output.importPage(input.getPage(page-1));
 output.save(pdf);
 }
 runOnUiThread(()->{produced=pdf;info.setText("جاهز: "+chosen.size()+" صفحة");
 if(action==0)print(pdf,chosen.size());if(action==1)save();if(action==2)share(pdf);
 });
 }catch(Exception ex){fail(ex);}}).start();
 }catch(Exception ex){fail(ex);}
 }
 void save(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,"Almimar-Selected-Pages.pdf");startActivityForResult(i,SAVE);}
 void share(File pdf){
 Uri uri=FileProvider.getUriForFile(this,getPackageName()+".provider",pdf);
 Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/pdf");i.putExtra(Intent.EXTRA_STREAM,uri);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
 startActivity(Intent.createChooser(i,"مشاركة ملف الطباعة"));
 }
 void print(File pdf,int count){
 PrintManager p=(PrintManager)getSystemService(PRINT_SERVICE);
 p.print("المعمار",new PrintDocumentAdapter(){
 @Override public void onLayout(PrintAttributes a,PrintAttributes b,android.os.CancellationSignal c,LayoutResultCallback cb,Bundle extra){
 cb.onLayoutFinished(new PrintDocumentInfo.Builder("Pages.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(count).build(),true);}
 @Override public void onWrite(PageRange[] ranges,android.os.ParcelFileDescriptor dest,android.os.CancellationSignal cancel,WriteResultCallback cb){
 try(InputStream in=new FileInputStream(pdf);OutputStream out=new FileOutputStream(dest.getFileDescriptor())){copy(in,out);cb.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});}catch(Exception e){cb.onWriteFailed(e.getMessage());}
 }
 },null);
 }
}