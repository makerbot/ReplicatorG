package replicatorg.dualstrusion;

/**
 * 
 * @author Noah Levy
 * This enum is a replacement for using an int or string to represent Tooheads, it is essential the order is not changed because Layer_Helper uses .ordinal() extensively
 * 
 * 
 *
 */
public enum Toolheads
{
	Secondary, Primary //DO NOT CHANGE THIS ORDER!!!!!!!!!! The code often depends on .ordinal() and you would cause a cataclysmic chain of events
}