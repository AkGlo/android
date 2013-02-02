package com.artifex.mupdf;

import android.net.Uri;

public class LinkInfoExternal extends LinkInfo {
	final public String url;

	public LinkInfoExternal(float l, float t, float r, float b, String u) {
		super(l, t, r, b);
		url = u;
	}

	public void acceptVisitor(LinkInfoVisitor visitor) {
		visitor.visitExternal(this);
	}
	
	public boolean isMediaURI() {
		return url.startsWith("http") 
				&& (url.contains("youtube") 
						|| url.contains("vimeo") 
						|| url.contains("localhost")
				);
	}

	public boolean isAutoPlay() {
		return Uri.parse(url).getQueryParameter("waplay") != null 
				&& Uri.parse(url).getQueryParameter("waplay").equals("auto");
	}

	public boolean isFullScreen() {
		return Uri.parse(url).getQueryParameter("warect") != null 
				&& Uri.parse(url).getQueryParameter("warect").equals("full");
	}

	public boolean isExternal() {
		return url.startsWith("http://localhost/");
	}

	public boolean hasVideoData() {
		return url.contains("mp4");
	}

	public boolean isImageFormat() {
		final String path = Uri.parse(url).getPath();
		return path.endsWith("jpg") 
				|| path.endsWith("png") 
				|| path.endsWith("bmp");
	}

	public boolean isVideoFormat() {
		final String path = Uri.parse(url).getPath();
		return path.endsWith("mp4");
	}

	@Override
	public String toString() {
		return "LinkInfo ["
				+ "isVideoFormat=" + isVideoFormat() 
				+ ", isImageFormat=" + isImageFormat() 
				+ ", hasVideoData=" + hasVideoData() 
				+ ", isExternal=" + isExternal() 
				+ ", isFullScreen=" + isFullScreen() 
				+ ", isAutoPlay=" + isAutoPlay() 
				+ ", uri=" + url
				+ "]";
	}
}
