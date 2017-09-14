package com.evolveum.midpoint.web.component.prism;

public class PrismWrapper {

	
	 	private boolean showEmpty;
	    private boolean minimalized;
	    private boolean sorted;
	    private boolean showMetadata;
	
	    public boolean isMinimalized() {
	        return minimalized;
	    }

	    public void setMinimalized(boolean minimalized) {
	        this.minimalized = minimalized;
	    }

	    public boolean isSorted() {
	        return sorted;
	    }

	    public void setSorted(boolean sorted) {
	        this.sorted = sorted;
	    }

	    public boolean isShowMetadata() {
	        return showMetadata;
	    }

	    public void setShowMetadata(boolean showMetadata) {
	        this.showMetadata = showMetadata;
	    }

	    public boolean isShowEmpty() {
	        return showEmpty;
	    }

	    public void setShowEmpty(boolean showEmpty) {
	        this.showEmpty = showEmpty;
//	        computeStripes();
	    }
}
