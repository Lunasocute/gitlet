package gitlet;

import java.io.File;
import java.util.List;

import static gitlet.Repository.*;
import static gitlet.Utils.*;


/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Luna Tian
 */
public class Main {
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2>....
     */

    public static void main(String[] args) {
        if (args.length == 0) {
            exitWithError("Please enter a command.");
        }
        String firstArg = args[0];
        if (!GITLET_DIR.exists() && !firstArg.equals("init")) {
            exitWithError("Not in an initialized Gitlet directory.");
        }
        switch(firstArg) {
            case "init":
                validNumArgs(args, 1);
                Repository.setupInit();
                break;
            case "add":
                validNumArgs(args, 2);
                Repository.addFile(args[1]);
                break;
            case "commit":
                validNumArgs(args, 2);
                Repository.commit(args[1]);
                break;
            case "rm":
                validNumArgs(args, 2);
                Repository.remove(args[1]);
                break;
            case "checkout":
                if (args.length == 3) {                 //redundant version, need reorganize;
                    Repository.checkout(args[1], args[2]);
                } else if (args.length == 4) {
                    Repository.checkout(args[1], args[2], args[3]);
                } else if (args.length == 2) {
                    Repository.checkout(args[1]);
                } else {
                    exitWithError("Incorrect operands.");
                }
                break;
            case "log":
                validNumArgs(args, 1);
                Repository.printLog(currCom());
                break;
            case "global-log":
                validNumArgs(args, 1);
                Repository.printAllLog();
                break;
            case "find":
                validNumArgs(args, 2);
                Repository.find(args[1]);
                break;
            case "status":
                validNumArgs(args, 1);
                Repository.status();
                break;
            case "branch":
                validNumArgs(args, 2);
                Branch.addBranch(args[1]);
                break;
            case "rm-branch":
                validNumArgs(args, 2);
                Branch.rmBranch(args[1]);
                break;
            case "reset":
                validNumArgs(args, 2);
                Repository.reset(args[1]);
                break;
            case "merge":
                validNumArgs(args, 2);
                Repository.merge(args[1]);
                break;
            case "add-remote":
                validNumArgs(args, 3);
                Repository.addremote(args[1], args[2]);
                break;
            case "rm-remote":
                validNumArgs(args, 2);
                Repository.rmremote(args[1]);
                break;
            case "push":
                validNumArgs(args, 3);
                Repository.push(args[1], args[2]);
                break;
            case "fetch":
                validNumArgs(args, 3);
                Repository.fetch(args[1], args[2]);
                break;
            case "pull":
                validNumArgs(args, 3);
                Repository.pull(args[1], args[2]);
                break;
            default:
                exitWithError("No command with that name exists.");
        }
    }

    public static void exitWithError(String message) {    //@source: lab6 Util.exitWithError
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(0);
    }

    public static void validNumArgs(String[] args, int n) {
        if (args.length != n) {
            exitWithError("Incorrect operands.");
        }
    }
}
