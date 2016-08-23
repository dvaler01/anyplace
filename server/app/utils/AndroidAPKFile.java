package utils;

import java.io.File;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

// Android apks are in the form:
// anyplace_android_vX.Y.Z_{DEV,RELEASE}.apk
//
public class AndroidAPKFile {

    public static class AndroidAPKComparator implements Comparator<AndroidAPKFile>{
        @Override
        public int compare(AndroidAPKFile thiss,AndroidAPKFile that) {
            try{
                String vThis = thiss.getVersion().substring(1);
                String vThat = that.getVersion().substring(1);

                String[] segsThis = vThis.split("[.]");
                String[] segsThat = vThat.split("[.]");
                for( int i=0; i<segsThis.length; i++ ){
                    int a = Integer.parseInt(segsThis[i]);
                    int b = Integer.parseInt(segsThat[i]);
                    if( a < b ){
                        return -1;
                    }else if( a > b ){
                        return 1;
                    }
                }
                return thiss.isRelease()?-1:1;
            }catch(NumberFormatException e){
                return -1;
            }
        }
    }


    private File mFile;
    private String mFileBasename;
    private String mUrl;

    private String mVersion;
    private boolean mIsRelease;
    private boolean mIsDev;

    private Date mDate;


    public AndroidAPKFile(File file){
        mFile = file;
        mDate = new Date(file.getAbsoluteFile().lastModified());
        mFileBasename = mFile.getAbsolutePath().substring( mFile.getAbsolutePath().lastIndexOf(File.separatorChar)+1 );
        String[] segs = mFileBasename.split("_");
        mVersion = segs[2];
        mIsRelease = segs[3].toLowerCase(Locale.ENGLISH).contains("release");
        mIsDev = !mIsRelease;
    }

    public String getVersion(){
        return mVersion;
    }

    public boolean isRelease(){
        return mIsRelease;
    }

    public boolean isDev(){
        return mIsDev;
    }

    public File getFile(){
        return mFile;
    }

    public Date getDate(){
        return mDate;
    }

    public String getFilePath(){
        return mFile.getAbsolutePath();
    }

    public String getFilePathBasename(){
        return mFileBasename;
    }

    public void setDownloadUrl(String url){
        mUrl = url;
    }
    public String getDownloadUrl(){
        return mUrl;
    }

}
