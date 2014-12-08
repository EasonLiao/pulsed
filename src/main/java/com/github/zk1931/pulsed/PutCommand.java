package com.github.zk1931.pulsed;

import com.github.zk1931.pulsed.DataTree.DirectoryNode;
import com.github.zk1931.pulsed.DataTree.InvalidPath;
import com.github.zk1931.pulsed.DataTree.NodeAlreadyExist;
import com.github.zk1931.pulsed.DataTree.NotDirectory;
import com.github.zk1931.pulsed.DataTree.PathNotExist;
import com.github.zk1931.pulsed.DataTree.TreeException;
import com.github.zk1931.pulsed.DataTree.VersionNotMatch;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.AsyncContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command for put, if file exists then update data of file, otherwise
 * creates a new file.
 */
public class PutCommand extends Command {

  private static final long serialVersionUID = 0L;
  private static final Logger LOG = LoggerFactory.getLogger(PutCommand.class);

  final String path;
  final byte[] data;
  final boolean recursive;
  final long version;

  public PutCommand(String path, byte[] data, boolean recursive, long version) {
    this.path = path;
    this.data = data.clone();
    this.recursive = recursive;
    this.version = version;
  }

  Node execute(DataTree tree)
      throws PathNotExist, InvalidPath, VersionNotMatch, DirectoryNode,
             NotDirectory, NodeAlreadyExist {

    if (version < -1) {
      // If version is less than -1 then we do creation or set depends on if the
      // path exists in the tree or not.
      if (tree.exist(this.path)) {
        // If the node exists, treat the command as request of update.
        return tree.setData(this.path, this.data, -1);
      } else {
        // Otherwise treat the command as request of creation.
        return tree.createFile(this.path, this.data, -1, recursive);
      }
    } else if (version == -1) {
      // If the version is -1 then we can only do creation.
      return tree.createFile(this.path, this.data, -1, recursive);
    } else {
      return tree.setData(this.path, this.data, version);
    }
  }

  void executeAndReply(DataTree tree, Object ctx) {
    AsyncContext context = (AsyncContext)ctx;
    HttpServletResponse response = (HttpServletResponse)(context.getResponse());
    try {
      Node node = execute(tree);
      Utils.setHeader(node, response);
      if (node.version == 0) {
        Utils.replyCreated(response, context);
      } else {
        Utils.replyOK(response, context);
      }
    } catch (PathNotExist ex) {
      Utils.replyNotFound(response, ex.getMessage(), context);
    } catch (VersionNotMatch ex) {
      Utils.replyConflict(response, ex.getMessage(), context);
    } catch (TreeException ex) {
      Utils.replyBadRequest(response, ex.getMessage(), context);
    }
  }
}
