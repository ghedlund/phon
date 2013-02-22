package ca.phon.session;

/**
 * Name and grouped information for a tier.
 *
 */
public interface TierDescription {
	
	/**
	 * Is the tier grouped, if tier is not grouped,
	 * {@link Tier#numberOfGroups()} will always return 1.
	 * 
	 * @return <code>true</code> if the tier is grouped, <code>false</code>
	 *  otherwise
	 */
	public boolean isGrouped();
	
	/**
	 * Get the name of the tier.
	 * 
	 * @return name of the tier
	 */
	public String getName();

}
