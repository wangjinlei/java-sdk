package com.qiniu.qbox.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONException;
import org.json.JSONStringer;

import com.qiniu.qbox.Config;

public class PutPolicy {
	
	public String scope;
	public String callbackUrl;
	public String returnUrl;
	public long expiry;
	
	public PutPolicy(String scope, long expires) {
		
		this.scope = scope;
		this.expiry = System.currentTimeMillis() / 1000 + expires;
	}

	public void setCallbackUrl(String callbackUrl) {
		this.callbackUrl = callbackUrl;
	}

	public void setReturnUrl(String returnUrl) {
		this.returnUrl = returnUrl;
	}

	public String marshal() throws JSONException {

		JSONStringer stringer = new JSONStringer();
		stringer.object();
		stringer.key("scope").value(this.scope);
		if (this.callbackUrl != null) {
			stringer.key("callbackUrl").value(this.callbackUrl);
		}
		if (this.returnUrl != null) {
			stringer.key("returnUrl").value(this.returnUrl);
		}
		stringer.key("deadline").value(this.expiry);
		stringer.endObject();

		return stringer.toString();
	}

	public byte[] makePutAuthToken() throws Exception {

		byte[] accessKey = Config.ACCESS_KEY.getBytes();
		byte[] secretKey = Config.SECRET_KEY.getBytes();
		
		try {
			String policyJson = this.marshal();
			byte[] policyBase64 = Client.urlsafeEncodeBytes(policyJson.getBytes());

			Mac mac = Mac.getInstance("HmacSHA1");
			SecretKeySpec keySpec = new SecretKeySpec(secretKey, "HmacSHA1");
			mac.init(keySpec);

			byte[] digest = mac.doFinal(policyBase64);
			byte[] digestBase64 = Client.urlsafeEncodeBytes(digest);			
			byte[] token = new byte[accessKey.length + 30 + policyBase64.length];

			System.arraycopy(accessKey, 0, token, 0, accessKey.length);
			token[accessKey.length] = ':';
			System.arraycopy(digestBase64, 0, token, accessKey.length + 1, digestBase64.length);
			token[accessKey.length + 29] = ':';
			System.arraycopy(policyBase64, 0, token, accessKey.length + 30, policyBase64.length);

			return token;
		} catch (Exception e) {
			throw new Exception("Fail to get qiniu put policy!", e);
		}
	}

	public String token() throws Exception {
		byte[] authToken = this.makePutAuthToken();
		return new String(authToken);
	}

}