package com.github.xiaolyuh;

import com.github.xiaolyuh.utils.CollectionUtils;
import com.github.xiaolyuh.utils.ConfigUtil;
import com.github.xiaolyuh.utils.NotifyUtil;
import com.google.common.collect.Lists;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Key;
import git4idea.commands.*;
import git4idea.i18n.GitBundle;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yuhao.wang3
 * @since 2020/4/7 14:36
 */
public class GitImpl implements Git {
    private git4idea.commands.Git git = git4idea.commands.Git.getInstance();

    @NotNull
    @Override
    public GitCommandResult checkout(@NotNull GitRepository repository, @NotNull String reference) {
        // 切换分支
        NotifyUtil.notifyGitCommand(repository.getProject(), String.format("git -c core.quotepath=false -c log.showSignature=false checkout %s --force", reference));
        return git.checkout(repository, reference, null, true, false);
    }

    @Override
    public GitCommandResult checkoutNewBranch(GitRepository repository, String branchName) {
        // git checkout -b 本地分支名x origin/远程分支名x
        final GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.CHECKOUT);
        h.setSilent(false);
        h.setStdoutSuppressed(false);
        h.addParameters("-b");
        h.addParameters(branchName);
        h.addParameters("origin/" + branchName);
        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
        return git.runCommand(h);
    }

    @Override
    public GitCommandResult fetchNewBranchByRemoteMaster(GitRepository repository, String master, String newBranchName) {
        //git fetch origin 远程分支名x:本地分支名x
        GitRemote remote = getDefaultRemote(repository);
        GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.FETCH);
        h.setSilent(false);
        h.setStdoutSuppressed(false);
        h.setUrls(remote.getUrls());
        h.addParameters("origin");
        // 远程分支名x:本地分支名x
        h.addParameters(master + ":" + newBranchName);
        h.addParameters("-f");

        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
        return git.runCommand(h);
    }


    @Override
    public GitCommandResult renameBranch(@NotNull GitRepository repository,
                                         @Nullable String oldBranch,
                                         @NotNull String newBranchName) {

        return git.renameBranch(repository, oldBranch, newBranchName);
    }

    @Override
    public GitCommandResult push(GitRepository repository, String localBranchName, boolean isNewBranch) {
        return push(repository, localBranchName, localBranchName, isNewBranch);
    }

    /**
     * push 本地分支到远程
     *
     * @param repository       gitRepository
     * @param localBranchName  分支名称
     * @param remoteBranchName 是否是新建分支
     * @return
     */
    @Override
    public GitCommandResult push(GitRepository repository, String localBranchName, String remoteBranchName, boolean isNewBranch) {
        GitRemote remote = getDefaultRemote(repository);
        GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.PUSH);
        h.setSilent(false);
        h.setStdoutSuppressed(false);
        h.setUrls(remote.getUrls());
        h.addParameters("origin");
        h.addParameters(localBranchName + ":" + remoteBranchName);
        h.addParameters("--tag");
        if (isNewBranch) {
            h.addParameters("--set-upstream");
        }

        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
        return git.runCommand(h);
    }

    @Override
    public GitCommandResult deleteRemoteBranch(@NotNull GitRepository repository, @Nullable String branchName) {
        GitRemote remote = getDefaultRemote(repository);
        GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.PUSH);
        h.setSilent(false);
        h.setStdoutSuppressed(false);
        h.setUrls(remote.getUrls());
        h.addParameters("origin");
        h.addParameters("--delete");
        h.addParameters(branchName);

        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
        return git.runCommand(h);
    }

    @Override
    public GitCommandResult deleteLocalBranch(@NotNull GitRepository repository, @Nullable String branchName) {
        // 删除本地分支
        NotifyUtil.notifyGitCommand(repository.getProject(), String.format("git -c core.quotepath=false -c log.showSignature=false branch -D %s", branchName));
        return git.branchDelete(repository, branchName, true);
    }

    @Override
    public GitCommandResult showRemoteLastCommit(@NotNull GitRepository repository, @Nullable String remoteBranchName) {
        //git show origin/master -s --format=Author:%ae-Date:%ad-Message:%s --date=format:%Y-%m-%d_%H:%M:%S
        GitRemote remote = getDefaultRemote(repository);
        GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.SHOW);
        h.setSilent(false);
        h.setStdoutSuppressed(false);
        h.setUrls(remote.getUrls());
        h.addParameters("origin/" + remoteBranchName);
        h.addParameters("-s");
        h.addParameters("--format=Author:%ae-Date:%ad-Message:%s");
        h.addParameters("--date=format:%Y-%m-%d_%H:%M:%S");

        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
        return git.runCommand(h);
    }


    @Override
    public GitCommandResult createNewTag(@NotNull GitRepository repository, @Nullable String tagName, @Nullable String message) {
        final GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.TAG);
        h.setSilent(false);
        h.addParameters("-a");
        h.addParameters("-f");
        h.addParameters("-m");
        h.addParameters(message);
        h.addParameters(ConfigUtil.getConfig(repository.getProject()).get().getTagPrefix() + tagName);

        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
        return git.runCommand(h);
    }

    @Override
    public GitCommandResult tagList(@NotNull GitRepository repository) {
        GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.TAG);
        h.setSilent(true);
        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());

        return ProgressManager.getInstance()
                .runProcessWithProgressSynchronously(() -> git.runCommand(h),
                        GitBundle.getString("tag.getting.existing.tags"),
                        false,
                        repository.getProject());
    }

    @Override
    public GitCommandResult fetch(@NotNull GitRepository repository) {
        NotifyUtil.notifyGitCommand(repository.getProject(), String.format("git -c core.quotepath=false -c log.showSignature=false fetch origin"));
        return git.fetch(repository, getDefaultRemote(repository), Collections.singletonList(new GitImpl.GitFetchPruneDetector()), new String[0]);
    }

    @Override
    public GitCommandResult pull(GitRepository repository, @Nullable String branchName) {
        GitRemote remote = getDefaultRemote(repository);
        GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.PULL);
        h.setSilent(false);
        h.setStdoutSuppressed(false);
        h.setUrls(remote.getUrls());
        h.addParameters("origin");
        h.addParameters(branchName + ":" + branchName);

        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
        return git.runCommand(h);
    }

    @Override
    public GitCommandResult merge(@NotNull GitRepository repository, @NotNull String branchToMerge, @NotNull GitLineHandlerListener... listeners) {
        NotifyUtil.notifyGitCommand(repository.getProject(),
                String.format("git -c core.quotepath=false -c log.showSignature=false merge %s ['%s' merge into '%s']", branchToMerge, branchToMerge, repository.getCurrentBranch()));
        return git.merge(repository, branchToMerge, Lists.newArrayList(), listeners);
    }

    @Override
    public GitCommandResult getUserEmail(GitRepository repository) {
        GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.CONFIG);
        h.setSilent(true);
        h.addParameters("--null", "--get", "user.email");
        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
        return git.runCommand(h);
    }

    private GitRemote getDefaultRemote(@NotNull GitRepository repository) {
        Collection<GitRemote> remotes = repository.getRemotes();
        if (CollectionUtils.isEmpty(remotes)) {
            return null;
        }
        return remotes.iterator().next();
    }

    private class GitFetchPruneDetector extends GitLineHandlerAdapter {

        private final Pattern PRUNE_PATTERN = Pattern.compile("\\s*x\\s*\\[deleted\\].*->\\s*(\\S*)");

        @NotNull
        private final Collection<String> myPrunedRefs = new ArrayList<>();

        @Override
        public void onLineAvailable(String line, Key outputType) {
            //  x [deleted]         (none)     -> origin/frmari
            Matcher matcher = PRUNE_PATTERN.matcher(line);
            if (matcher.matches()) {
                myPrunedRefs.add(matcher.group(1));
            }
        }

        @NotNull
        public Collection<String> getPrunedRefs() {
            return myPrunedRefs;
        }
    }
}