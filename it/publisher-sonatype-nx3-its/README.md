## Publisher Sonatype Nx3 IT

This IT in current state of affairs cannot run in automated fashion.

```
$ chown -R 200 docker/data
```

is needed (plus, commenting out 109-111 lines in src/it/scripts/DockerLib.groovy).

And even then IT will fail, as it attempts to deploy same release twice into Nx3, once
with Maven 3.9.x and once with Maven 4.x (and redeploy is refused).

Circumvention: either start up Nx3 initially, and reconfigure it allow to redeploy, or 
run ITs one by one :shrug: