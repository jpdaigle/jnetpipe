## JNetPipe
JNetPipe is a TCP/IP forwarding tunnel and protocol testing tool for 
simulating TCP connection failures.

Jean-Philippe Daigle ([jpdaigle@gmail.com](mailto://jpdaigle@gmail.com))

## USAGE
    // TODO: (complete)

JNetPipe, in its default run mode, operates pretty much like an SSH tunnel: it hooks up a 
specified local port to a remote address and port. That's it. You can use it for port forwarding 
like so, the tunnel syntax even mirrors SSH's:

    java JNetPipe <LOCALPORT>:<REMOTEHOST>:<REMOTEPORT>

Where it gets interesting is when you use it in interactive mode with the embedded BeanShell:

    java JNetPipe <LOCALPORT>:<REMOTEHOST>:<REMOTEPORT> --shell

This will create a tunnel and launch a shell where tunnels can be examined, 
started and stopped, stats can be printed, etc.

## STATUS
This software should be considered (very) experimental. Interfaces, both
private and public, are still subject to change as the architecture of this
tool matures.

This tool was created to aid me in performing ad-hoc testing of network failure
scenarios when developing network software at [my day job](http://www.solacesystems.com). My employer 
graciously accepted I release it as open source.

## LICENSE
JNetPipe is available under a BSD License. See 
<tt>license.BSD.txt</tt> in this directory.

Third-party JARs are redistributed under their respective 
licenses, see <tt>/lib</tt>. Specifically, 

* BeanShell is redistributed under the LGPL
* Log4J is redistributed under Apache License 2.0
