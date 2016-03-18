package za.dams.paracrm;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class SyncPullRequest implements Parcelable {
	
	public static class SyncPullRequestFileCondition {
		public String fileFieldCode ;
		public String conditionSign ;
		public String conditionValue ;
		public List<String> conditionValueArr ;
		
		public SyncPullRequestFileCondition() {
			fileFieldCode = "";
			conditionSign = "";
			conditionValue = "";
			conditionValueArr = new ArrayList<String>();
		}
		
		public boolean equals( Object o ) {
			SyncPullRequestFileCondition sprfc = (SyncPullRequestFileCondition)o ;
			if( !this.fileFieldCode.equals(sprfc.fileFieldCode) ) {
				return false ;
			}
			if( !this.conditionSign.equals(sprfc.conditionSign) ) {
				return false ;
			}
			if( !this.conditionValue.equals(sprfc.conditionValue) ) {
				return false ;
			}
			if( !this.conditionValueArr.equals(sprfc.conditionValueArr) ) {
				return false ;
			}
			return true ;
		}
		public int hashCode() {
			int result = 17 ;
			
			result = 31 * result + fileFieldCode.hashCode() ;
			result = 31 * result + conditionSign.hashCode() ;
			result = 31 * result + conditionValue.hashCode() ;
			result = 31 * result + conditionValueArr.hashCode() ;
			
			return result ;
		}
	}
	
	public String fileCode ;
	public ArrayList<SyncPullRequestFileCondition> fileConditions ;
	public int limitResults ;
	public boolean supplyTimestamp ;
	
    public static final Parcelable.Creator<SyncPullRequest> CREATOR = new Parcelable.Creator<SyncPullRequest>() {
        public SyncPullRequest createFromParcel(Parcel in) {
            return new SyncPullRequest(in);
        }

        public SyncPullRequest[] newArray(int size) {
            return new SyncPullRequest[size];
        }
    };

	@Override
	public int describeContents() {
		// Alsmost dummy method
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(fileCode) ;
		out.writeInt(fileConditions.size()) ;
		for( SyncPullRequestFileCondition prfc : fileConditions ) {
			out.writeString(prfc.fileFieldCode);
			out.writeString(prfc.conditionSign);
			out.writeString(prfc.conditionValue);
			out.writeInt(prfc.conditionValueArr.size()) ;
			for( String s : prfc.conditionValueArr ) {
				out.writeString(s);
			}
		}
		out.writeInt(limitResults);
		out.writeByte( (byte)(supplyTimestamp ? 1:0) );
	}
	public SyncPullRequest() {
		fileCode = "" ;
		fileConditions = new ArrayList<SyncPullRequestFileCondition>() ;
		limitResults = 0 ;
		supplyTimestamp = true ;
	}
	public SyncPullRequest( Parcel in ) {
		fileCode = in.readString() ;
		fileConditions = new ArrayList<SyncPullRequestFileCondition>() ;
		int nbConditions = in.readInt() ;
		for( int idx=0 ; idx<nbConditions ; idx++ ) {
			SyncPullRequestFileCondition prfc = new SyncPullRequestFileCondition() ;
			prfc.fileFieldCode = in.readString() ;
			prfc.conditionSign = in.readString() ;
			prfc.conditionValue = in.readString() ;
			int nbValueArr = in.readInt();
			for( int idxb=0 ; idxb<nbValueArr ; idxb++ ) {
				prfc.conditionValueArr.add(in.readString()) ;
			}
			fileConditions.add(prfc) ;
		}
		limitResults = in.readInt() ;
		supplyTimestamp = (in.readByte() == 1) ;
	}
	
	public boolean equals( Object o ) {
		SyncPullRequest spr = (SyncPullRequest)o ;
		if( !this.fileCode.equals(spr.fileCode) ) {
			return false ;
		}
		if( !this.fileConditions.equals(spr.fileConditions) ) {
			return false ;
		}
		if( !(this.limitResults == spr.limitResults) ) {
			return false ;
		}
		if( !(this.supplyTimestamp == spr.supplyTimestamp) ) {
			return false ;
		}
		return true ;
	}
	public int hashCode() {
		int result = 17 ;
		result = 31 * result + fileCode.hashCode() ;
		result = 31 * result + fileConditions.hashCode() ;
		result = 31 * result + limitResults ;
		result = 31 * result + (supplyTimestamp ? 1 : 0);
		return result ;
	}

}
