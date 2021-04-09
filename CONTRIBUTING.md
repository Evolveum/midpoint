MidPoint is open to contributions. Any contribution is appreciated whether it is documentation update, localization, bugfix or a new feature. Anyone can start experimenting with midPoint code without the need to ask for any privileges or access to our source code repository. Git distributed development model allows this to be done easily. This pages describes how to do it. It describes how to create and contribute a simple bugfix and also how to cooperate with the midPoint team in a longer run.

Communication
=============

Communication with core midPoint development team is crucial for any successful participation in midPoint development. There are several methods to communicate with the core team:
* [Mailing lists](https://docs.evolveum.com/community/mailing-lists/) are general-purpose communication mechanisms for midPoint community. All mailing lists are public. Almost any conversation is allowed on mailing lists as long as it is kept reasonable and polite. This includes conversation regarding development topics. However, the nature of a mailing list can make the conversation somehow slow and inflexible.
* [Bug-tracking System](https://docs.evolveum.com/support/bug-tracking-system/) is open to anyone to search, open a bug report or participate on existing bug reports.
* [Development chat room](https://docs.evolveum.com/community/development/development-chat/) on gitter can be used as a faster communication channel. The room is public. However, the room is limited to development conversations. It is OK to discuss your contribution, planned contribution, development idea, localization, documentation updates and similar topics on this chat. However, the chat room is not supposed to be a replacement for subscription and support.
* There are other private communication channels available for midPoint subscribers - and platform subscribers in particular. Please contact Evolveum for the details.

Git
===

Evolveum maintains midPoint repository on GitHub:

[https://github.com/Evolveum/midpoint](https://github.com/Evolveum/midpoint)

This is the primary midPoint repository. GitHub users may use this repository to submit a contribution. This is the easiest way how individual developers can contribute to midPoint. Simply fork midPoint project on github, make the changes and when finished create a pull request. Please see github documentation for the details.

Tips and Best Practice
======================
* Do not submit each individual commit unless the commit itself is a complete contribution. If your contribution is divided into several commits (which is perfectly fine) then send all the commits together so the maintainer can apply and test them together.
* If you have many commits but you want only to show them as one in the final midPoint history you might want to squash these commits to one. You can use git interactive rebasing to do this. (git rebase -i, the Git book provides more details)
* Provide a meaningful commit message. If the commit message is longer than a single line provide a short summary of the message in the first line and then provide more details in subsequent lines. Most git tools display just the first line of the commit message therefore the developers should be able to get an idea about the commit just from the first line.
* If there is an "issue" created in our bug-tracking system it is recommended to include issue identifier in the commit message.
* You may want to create a topic branch for larger contributions.
* There is no code ownership principle. Not in the midPoint development team and we do not provide that to the contributions as well. All code "belongs" to every developer and anyone has the "right" to modify any code. The only thing that we care about is the quality of the modification, not its origin. Therefore feel free to modify any code and fix bugs anywhere in the midPoint core or in any of the contributions. Just please make sure you know what you are doing. If you are not you are free to discuss that on midpoint-dev mailing list. If you contribute a code be prepared that others may modify it. If you do not want others to "ruin" you code then do not contribute it.
* MidPoint core team is trying to be quite careful about the state of the master branch in main midPoint repository. We try very hard not to break the build and we are also careful about passing tests and overall code quality. But this is software development and we are only human beings. Therefore it may happen that we break something occasionally. Therefore it is good idea to check the state of the source code before pulling from the main midPoint repository. The easiest way to do this is by looking at our continuous integration system. If it is mostly green then it is probably OK to pull changes. If it is too red it is better to postpone the pull for a while.
* Also write tests (e.g. integration tests) not just the main code. If you are fixing a bug try to write a test for the bug first and fix the bug second. For larger pieces of functionality try to create a fair amount of test code. Submit the tests as part of your contribution. You write the tests for your own good. As there is no code ownership anyone might (unintentionally) break your code. If you have good tests for the code the problem will be detected soon after the modification while it is still easy to fix. If you have no tests then you code will break without anyone noticing it for quite a long time. This will cause that your contribution might "corrode" over time and it may even be removed from the main code if its quality drops too low.
* For more details about contributing using git please see the "Distributed Git - Contributing to a Project" chapter of the Git book. In fact the whole book is more than worth reading.


Contributor License Agreements
==============================
MidPoint is dual licensed under Apache License 2.0 and European Union Public License 1.2. MidPoint users may choose any of those two licenses for their use of midPoint.

But the situation is more complicated for the contributors. While midPoint was single-licensed, the intent of a contributor to contribute under that license was quite clear. However, if users may to choose which license to use, contributors might be able to choose as well. And that may lead to confusion and uncertainty about midPoint licensing and other legal issues.

Therefore it is necessary to require contributor license agreements (CLA) from midPoint contributors. The purpose of the license agreements is to make licensing of midPoint code completely clear. Therefore if you submit a contribution to midPoint you will be asked to sign a CLA before the contribution can be accepted.


Credit
======

Git maintains the commit meta-data of the original commit. And this is what will be recorded in the history trail of main midPoint repository. Therefore the original contributor will be recorded in each commit. Apart from this the contributors are free to add their names to the appropriate place in the file header (e.g. Java @author annotation) if they feel their contribution is big enough to justify it.


Development Guidelines
======================

See [Development Guidelines](https://wiki.evolveum.com/display/midPoint/Development+Guidelines)
