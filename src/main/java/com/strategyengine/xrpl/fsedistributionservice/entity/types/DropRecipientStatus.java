package com.strategyengine.xrpl.fsedistributionservice.entity.types;



public enum DropRecipientStatus {

	//QUEUED ready to be sent
	//SENDING in the process of sending.  If stuck in this status, then we don't know if it was paid since an exception was thrown in the middle of sending
	//FAILED do not retry
	//COMPLETE verified sent
	QUEUED, SENDING, FAILED, VERIFIED
}
