package ca.phon.app.opgraph.report.tree;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;

public class ReportTreePath implements Iterable<ReportTreeNode> {

	private final ReportTreeNode[] path;
	
	public ReportTreePath(ReportTreeNode[] path) {
		this.path = path;
	}
	
	public ReportTreeNode[] getPath() {
		return this.path;
	}
	
	public ReportTreeNode lastChild() {
		return (path.length > 0 ? path[path.length -1] : null);
	}
	
	public ReportTreePath pathByAppendingChild(ReportTreeNode node) {
		ReportTreeNode newPath[] = Arrays.copyOf(path, path.length+1);
		newPath[newPath.length-1] = node;
		return new ReportTreePath(newPath);
	}
	
	public ReportTreePath pathWithNewParent(ReportTreeNode parent) {
		ReportTreeNode newPath[] = new ReportTreeNode[path.length+1];
		newPath[0] = parent;
		for(int i = 1; i < newPath.length; i++) {
			newPath[i] = path[i-1];
		}
		return new ReportTreePath(newPath);
	}
	
	public ReportTreePath pathByRemovingRoot() {
		ReportTreeNode newPath[] = Arrays.copyOfRange(path, 1, path.length);
		return new ReportTreePath(newPath);
	}
	
	@Override
	public String toString() {
		return (path.length > 0
					? Arrays.stream(path)
							.map( ReportTreeNode::getTitle )
							.collect( Collectors.joining("/") )
					: "");
	}

	@Override
	public Iterator<ReportTreeNode> iterator() {
		return Arrays.asList(getPath()).iterator();
	}

}
