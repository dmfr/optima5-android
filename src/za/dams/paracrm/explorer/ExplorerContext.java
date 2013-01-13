package za.dams.paracrm.explorer;

import android.os.Parcel;
import android.os.Parcelable;
import za.dams.paracrm.BibleHelper;

public class ExplorerContext implements Parcelable {
	
	public static final int NO_MODE = 0 ;
	public static final int MODE_BIBLE = 1 ;
	public static final int MODE_FILE = 2 ;
	public static final int MODE_QUERY = 3 ;
	
	
	public final int mMode ;
	public final String mBibleCode ;
	public final String mFileCode ;
	public final BibleHelper.BibleEntry mSearchedBibleEntry ;
	
	private ExplorerContext( int mode, String bibleCode, String fileCode, BibleHelper.BibleEntry be ) {
		mMode = mode ;
		mBibleCode = bibleCode ;
		mFileCode = fileCode ;
		mSearchedBibleEntry = be ;
	}
	private ExplorerContext( Parcel in ) {
		mMode = in.readInt() ;
		mBibleCode = in.readString() ;
		mFileCode = in.readString() ;
		if( in.readByte() == 1 ) {
			mSearchedBibleEntry = in.readParcelable(BibleHelper.BibleEntry.class.getClassLoader()) ;
		} else {
			mSearchedBibleEntry = null ;
		}
	}

	
	
	public static ExplorerContext forNone()  {
		return new ExplorerContext(NO_MODE,null,null,null) ;
	}
	public static ExplorerContext forBible( String bibleCode )  {
		return new ExplorerContext(MODE_BIBLE,bibleCode,null,null) ;
	}
	public static ExplorerContext forBible( String bibleCode, BibleHelper.BibleEntry be )  {
		return new ExplorerContext(MODE_BIBLE,bibleCode,null,be) ;
	}
	public static ExplorerContext forFile( String fileCode )  {
		return new ExplorerContext(MODE_FILE,null,fileCode,null) ;
	}
	public static ExplorerContext forFile( String fileCode, BibleHelper.BibleEntry be )  {
		return new ExplorerContext(MODE_FILE,null,fileCode,be) ;
	}
	public static ExplorerContext forQuery()  {
		return new ExplorerContext(MODE_QUERY,null,null,null) ;
	}
	public static ExplorerContext forQuery( BibleHelper.BibleEntry be )  {
		return new ExplorerContext(MODE_QUERY,null,null,be) ;
	}
	
	public boolean isSearch() {
		if( mSearchedBibleEntry == null ) {
			return false ;
		}
		return true ;
	}
	
	
	public int describeContents() {
		return 0;
	}
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mMode);
		dest.writeString(mBibleCode);
		dest.writeString(mFileCode);
		dest.writeByte( (byte) ((mSearchedBibleEntry != null)? 1 : 0) ) ;
		if( mSearchedBibleEntry != null ) {
			dest.writeParcelable(mSearchedBibleEntry, 0) ;
		}
	}
	public static Parcelable.Creator<ExplorerContext> CREATOR =
			new Parcelable.Creator<ExplorerContext>() {
		@Override
		public ExplorerContext createFromParcel(Parcel source) {
			return new ExplorerContext(source);
		}

		@Override
		public ExplorerContext[] newArray(int size) {
			return new ExplorerContext[size];
		}
	};
	
	
}
