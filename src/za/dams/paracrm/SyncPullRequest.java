package za.dams.paracrm;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class SyncPullRequest implements Parcelable {
	
	public static class SyncPullRequestFileCondition {
		public String fileFieldCode ;
		public String conditionSign ;
		public String conditionValue ;
		
		public SyncPullRequestFileCondition() {}
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
			fileConditions.add(prfc) ;
		}
		limitResults = in.readInt() ;
		supplyTimestamp = (in.readByte() == 1) ;
	}

}
