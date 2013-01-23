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
	public final BibleHelper.BibleEntry mFilteredBibleEntry ;
	
	private ExplorerContext( int mode, String bibleCode, String fileCode, BibleHelper.BibleEntry be ) {
		mMode = mode ;
		mBibleCode = bibleCode ;
		mFileCode = fileCode ;
		mFilteredBibleEntry = be ;
	}
	private ExplorerContext( Parcel in ) {
		mMode = in.readInt() ;
		mBibleCode = in.readString() ;
		mFileCode = in.readString() ;
		if( in.readByte() == 1 ) {
			mFilteredBibleEntry = in.readParcelable(BibleHelper.BibleEntry.class.getClassLoader()) ;
		} else {
			mFilteredBibleEntry = null ;
		}
	}
	
	public boolean equals( Object o ) {
		if( o == null ) {
			return false ;
		}
		ExplorerContext ec = (ExplorerContext)o ;
		if( this.mMode != ec.mMode ) {
			return false ;
		}
		if( this.mBibleCode == null && ec.mBibleCode != null ) {
			return false ;
		}
		else if( this.mBibleCode != null && !this.mBibleCode.equals(ec.mBibleCode) ) {
			return false ;
		}
		if( this.mFileCode == null && ec.mFileCode != null ) {
			return false ;
		}
		else if( this.mFileCode != null && !this.mFileCode.equals(ec.mFileCode) ) {
			return false ;
		}
		if( this.mFilteredBibleEntry == null && ec.mFilteredBibleEntry != null ) {
			return false ;
		}
		else if( this.mFilteredBibleEntry != null && !this.mFilteredBibleEntry.equals(ec.mFilteredBibleEntry) ) {
			return false ;
		}
		return true ;
	}
	public int hashCode() {
		int result = 17 ;
		
		result = 31 * result + mMode ;
		if( mBibleCode != null ) {
			result = 31 * result + mBibleCode.hashCode() ;
		} else {
			result = 31 * result ;  
		}
		if( mFileCode != null ) {
			result = 31 * result + mFileCode.hashCode() ;
		} else {
			result = 31 * result ;  
		}
		if( mFilteredBibleEntry != null ) {
			result = 31 * result + mFilteredBibleEntry.hashCode() ;
		} else {
			result = 31 * result ;  
		}
		
		return result ;
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
	
	public boolean isFiltered() {
		if( mFilteredBibleEntry == null ) {
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
		dest.writeByte( (byte) ((mFilteredBibleEntry != null)? 1 : 0) ) ;
		if( mFilteredBibleEntry != null ) {
			dest.writeParcelable(mFilteredBibleEntry, 0) ;
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
