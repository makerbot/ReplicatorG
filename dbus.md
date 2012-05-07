# Printing via DBus

Printing via DBus is a two or four step process. For a single extruder print
the `.stl` file must be converted to `.gcode` by the toolpath generator and
then it must be printed by the printer. For a dual extruder print the left and
right `.stl` files must be individually converted to `.gcode` by the toolpath
generator, merged into a single `.gcode` file by the toolpath generator, and
then printed by the printer.

## ToopathGenerator

The toolpath generator interface exports two methods: `Generate` and
`GenerateDualStrusion`.

    Interface: com.makerbot.alpha.ToolpathGenerator1
      void Generate(string stl_filename)
      void GenerateDualStrusion(string left_gcode_filename, string right_gcode_filename, string merged_gcode_filename)

The Generate method converts an `.stl` file into a `.gcode` file. The output
filename and path are the same as the input filename and path except that the
`.stl` extension is replaced with `.gcode`.

The GenerateDualStrusion method merges two `.gcode` files into a third `.gcode`
file.

The interface exports two signals: `Progress` and `Complete`. The `Progress`
signal is issued repeatedly as the toolpath generator slices or merges the
model. The `Complete` signal is issued once the toolpath generator is done
slicing or merging. Neither signal carries any data.

A toolpath generator task is complete when it issues the `Complete` signal.

## Printer

The printer interface exports a `Build` method.

    Interface: com.makerbot.alpha.Printer1
      void Build(string gcode_filename)

The Build method builds the `.gcode` file on the printer.

The interface exports a `State` property. The `State` is encoded as a 32-bit
unsigned integer. The service issues a standard
`org.freedesktop.DBus.Properties.PropertiesChanged` signal whenever the value
changes.

<table>
  <thead>
    <tr>
      <th>State</th>
      <th>Meaning</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>0</td>
      <td>NotAttached</td>
    </tr>
    <tr>
      <td>1</td>
      <td>Connecting</td>
    </tr>
    <tr>
      <td>2</td>
      <td>Ready</td>
    </tr>
    <tr>
      <td>3</td>
      <td>Building</td>
    </tr>
    <tr>
      <td>4</td>
      <td>Paused</td>
    </tr>
    <tr>
      <td>5</td>
      <td>Error</td>
    </tr>
  </tbody>
</table>

The interface exports a `Progress` signal. It is issued repeatedly as the
printer executes gcode statements. The signal carries a `lines` property that
counts the number of gcode statements executed. It also carries a `totalLines`
property that counts the total number of gcode statements for the current
print.

A printer task is done when it moves from the `Building` state to the `Ready`
state.

## Examples

The examples below run a job using the `dbus-send` command. This isn't ideal
since `dbus-send` cannot listen for the various signals issued by the toolpath
generator and printer. It has no automatic way to detect when any task is done.
A proper DBus client does not have this problem.

### DBus Services

Start a toolpath generator process:

    $ ./replicatorg toolpathGenerator --bus-name com.makerbot.ToolpathGenerator

Start a printer process:

    $ ./replicatorg printer --bus-name com.makerbot.Printer

Both commands support an `--alternate-prefs` option. This is the same as the
standard (non-service) ReplicatorG option and is designed to be used in
conjunction with standard ReplicatorG. You can create alternate preferences
with standard ReplicatorG and then use them with the service ReplicatorG:

    $ cd ReplicatorG # not the service kind
    $ ./replicatorg --alternate-prefs xyzzy

      ... configure stuff in the ReplicatorG GUI ...

    $ cd ReplicatorG-service
    $ ./replicatorg --alternate-prefs xyzzy printer --bus-name com.makerbot.Printer

NOTE: The `--alternate-prefs` option must be specified *before* the subcommand.

Both commands support a `--machine-name` option. The machine name must match
the name of one of the available XML machine descriptions. For example:

    $ ./replicatorg toolpathGenerator --bus-name com.makerbot.Printer --machine-name 'The Replicator Dual'

The `printer` command also supports a `--port` option that lets you set the
name of the serial port. For example:

    $ ./replicatorg printer --bus-name com.makerbot.Printer --port /dev/ttyACM1

NOTE: The `--machine-name` and `--port` options must be specified *after* the
subcommand.

### Single Extruder

Convert an `.stl` file to `.gcode`:

    $ dbus-send --print-reply --session --dest=com.makerbot.ToolpathGenerator /com/makerbot/ToolpathGenerator com.makerbot.alpha.ToolpathGenerator1.Generate string:/path/to/something.stl

Print the `.gcode`:

    $ dbus-send --print-reply --session --dest=com.makerbot.Printer /com/makerbot/Printer com.makerbot.alpha.Printer1.Build string:/path/to/something.gcode

### Dual Extruder

Convert the two `.stl` files to `.gcode`:

    $ dbus-send --print-reply --session --dest=com.makerbot.ToolpathGenerator /com/makerbot/ToolpathGenerator com.makerbot.alpha.ToolpathGenerator1.Generate string:/path/to/left.stl
    $ dbus-send --print-reply --session --dest=com.makerbot.ToolpathGenerator /com/makerbot/ToolpathGenerator com.makerbot.alpha.ToolpathGenerator1.Generate string:/path/to/right.stl

Merge the two `.gcode` files:

    $ dbus-send --print-reply --session --dest=com.makerbot.ToolpathGenerator /com/makerbot/ToolpathGenerator com.makerbot.alpha.ToolpathGenerator1.GenerateDualStrusion string:/path/to/left.gcode string:/path/to/right.gcode string:/path/to/merged.gcode

Print the `.gcode`:

    $ dbus-send --print-reply --session --dest=com.makerbot.Printer /com/makerbot/Printer com.makerbot.alpha.Printer1.Build string:/path/to/merged.gcode
