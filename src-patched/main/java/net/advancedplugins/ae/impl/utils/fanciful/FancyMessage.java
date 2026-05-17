package net.advancedplugins.ae.impl.utils.fanciful;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;

public class FancyMessage implements JsonRepresentedObject, Cloneable, Iterable<MessagePart>, ConfigurationSerializable {
   private static final JsonParser _stringParser = new JsonParser();
   private List<MessagePart> messageParts = new ArrayList<>();
   private String jsonString;
   private boolean dirty;

   public FancyMessage(String firstPartText) {
      this(TextualComponent.rawText(firstPartText));
   }

   private FancyMessage(TextualComponent firstPartText) {
      this.messageParts.add(new MessagePart(firstPartText));
      this.jsonString = null;
      this.dirty = false;
   }

   private FancyMessage() {
      this((TextualComponent)null);
   }

   public static FancyMessage deserialize(Map<String, Object> serialized) {
      FancyMessage msg = new FancyMessage();
      msg.messageParts = (List<MessagePart>)serialized.get("messageParts");
      msg.jsonString = serialized.containsKey("JSON") ? serialized.get("JSON").toString() : null;
      msg.dirty = !serialized.containsKey("JSON");
      return msg;
   }

   private static FancyMessage deserialize(String json) {
      JsonObject serialized = _stringParser.parse(json).getAsJsonObject();
      JsonArray extra = serialized.getAsJsonArray("extra");
      FancyMessage returnVal = new FancyMessage();
      returnVal.messageParts.clear();

      for (JsonElement mPrt : extra) {
         MessagePart component = new MessagePart();
         JsonObject messagePart = mPrt.getAsJsonObject();

         for (Entry<String, JsonElement> entry : messagePart.entrySet()) {
            if (TextualComponent.isTextKey(entry.getKey())) {
               Map<String, Object> serializedMapForm = new HashMap<>();
               serializedMapForm.put("key", entry.getKey());
               if (entry.getValue().isJsonPrimitive()) {
                  serializedMapForm.put("value", entry.getValue().getAsString());
               } else {
                  for (Entry<String, JsonElement> compositeNestedElement : entry.getValue().getAsJsonObject().entrySet()) {
                     serializedMapForm.put("value." + compositeNestedElement.getKey(), compositeNestedElement.getValue().getAsString());
                  }
               }

               component.text = TextualComponent.deserialize(serializedMapForm);
            } else if (MessagePart.stylesToNames.inverse().containsKey(entry.getKey())) {
               if (entry.getValue().getAsBoolean()) {
                  component.styles.add((ChatColor)MessagePart.stylesToNames.inverse().get(entry.getKey()));
               }
            } else if (entry.getKey().equals("color")) {
               component.color = ChatColor.valueOf(entry.getValue().getAsString().toUpperCase(Locale.ROOT));
            } else if (entry.getKey().equals("clickEvent")) {
               JsonObject object = entry.getValue().getAsJsonObject();
               component.clickActionName = object.get("action").getAsString();
               component.clickActionData = object.get("value").getAsString();
            } else if (entry.getKey().equals("hoverEvent")) {
               JsonObject object = entry.getValue().getAsJsonObject();
               component.hoverActionName = object.get("action").getAsString();
               if (object.get("value").isJsonPrimitive()) {
                  component.hoverActionData = new JsonString(object.get("value").getAsString());
               } else {
                  component.hoverActionData = deserialize(object.get("value").toString());
               }
            } else if (entry.getKey().equals("insertion")) {
               component.insertionData = entry.getValue().getAsString();
            } else if (entry.getKey().equals("with")) {
               for (JsonElement object : entry.getValue().getAsJsonArray()) {
                  if (object.isJsonPrimitive()) {
                     component.translationReplacements.add(new JsonString(object.getAsString()));
                  } else {
                     component.translationReplacements.add(deserialize(object.toString()));
                  }
               }
            }
         }

         returnVal.messageParts.add(component);
      }

      return returnVal;
   }

   public FancyMessage clone() throws CloneNotSupportedException {
      FancyMessage instance = (FancyMessage)super.clone();
      instance.messageParts = new ArrayList<>(this.messageParts.size());

      for (int i = 0; i < this.messageParts.size(); i++) {
         instance.messageParts.add(i, this.messageParts.get(i).clone());
      }

      instance.dirty = false;
      instance.jsonString = null;
      return instance;
   }

   public FancyMessage text(String text) {
      MessagePart latest = this.latest();
      latest.text = TextualComponent.rawText(text);
      this.dirty = true;
      return this;
   }

   public FancyMessage text(TextualComponent text) {
      MessagePart latest = this.latest();
      latest.text = text;
      this.dirty = true;
      return this;
   }

   public FancyMessage color(ChatColor color) {
      if (!color.isColor()) {
         throw new IllegalArgumentException(color.name() + " is not a color");
      } else {
         this.latest().color = color;
         this.dirty = true;
         return this;
      }
   }

   public FancyMessage style(ChatColor... styles) {
      for (ChatColor style : styles) {
         if (!style.isFormat()) {
            throw new IllegalArgumentException(style.name() + " is not a style");
         }
      }

      this.latest().styles.addAll(Arrays.asList(styles));
      this.dirty = true;
      return this;
   }

   public FancyMessage file(String path) {
      this.onClick("open_file", path);
      return this;
   }

   public FancyMessage link(String url) {
      this.onClick("open_url", url);
      return this;
   }

   public FancyMessage suggest(String command) {
      this.onClick("suggest_command", command);
      return this;
   }

   public FancyMessage insert(String command) {
      this.latest().insertionData = command;
      this.dirty = true;
      return this;
   }

   public FancyMessage command(String command) {
      this.onClick("run_command", command);
      return this;
   }

   public FancyMessage achievementTooltip(String name) {
      this.onHover("show_achievement", new JsonString("achievement." + name));
      return this;
   }

   private FancyMessage tooltip(String text) {
      this.onHover("show_text", new JsonString(text));
      return this;
   }

   public FancyMessage tooltip(Iterable<String> lines) {
      this.tooltip(ArrayWrapper.toArray(lines, String.class));
      return this;
   }

   public FancyMessage tooltip(String... lines) {
      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < lines.length; i++) {
         builder.append(lines[i]);
         if (i != lines.length - 1) {
            builder.append('\n');
         }
      }

      this.tooltip(builder.toString());
      return this;
   }

   private FancyMessage formattedTooltip(FancyMessage text) {
      for (MessagePart component : text.messageParts) {
         if (component.clickActionData != null && component.clickActionName != null) {
            throw new IllegalArgumentException("The tooltip text cannot have click data.");
         }

         if (component.hoverActionData != null && component.hoverActionName != null) {
            throw new IllegalArgumentException("The tooltip text cannot have a tooltip.");
         }
      }

      this.onHover("show_text", text);
      return this;
   }

   private FancyMessage formattedTooltip(FancyMessage... lines) {
      if (lines.length < 1) {
         this.onHover(null, null);
         return this;
      } else {
         FancyMessage result = new FancyMessage();
         result.messageParts.clear();

         for (int i = 0; i < lines.length; i++) {
            try {
               for (MessagePart component : lines[i]) {
                  if (component.clickActionData != null && component.clickActionName != null) {
                     throw new IllegalArgumentException("The tooltip text cannot have click data.");
                  }

                  if (component.hoverActionData != null && component.hoverActionName != null) {
                     throw new IllegalArgumentException("The tooltip text cannot have a tooltip.");
                  }

                  if (component.hasText()) {
                     result.messageParts.add(component.clone());
                  }
               }

               if (i != lines.length - 1) {
                  result.messageParts.add(new MessagePart(TextualComponent.rawText("\n")));
               }
            } catch (CloneNotSupportedException var6) {
               Bukkit.getLogger().log(Level.WARNING, "Failed to clone object", (Throwable)var6);
               return this;
            }
         }

         return this.formattedTooltip(result.messageParts.isEmpty() ? null : result);
      }
   }

   public FancyMessage formattedTooltip(Iterable<FancyMessage> lines) {
      return this.formattedTooltip(ArrayWrapper.toArray(lines, FancyMessage.class));
   }

   public FancyMessage translationReplacements(String... replacements) {
      for (String str : replacements) {
         this.latest().translationReplacements.add(new JsonString(str));
      }

      this.dirty = true;
      return this;
   }

   private FancyMessage translationReplacements(FancyMessage... replacements) {
      Collections.addAll(this.latest().translationReplacements, replacements);
      this.dirty = true;
      return this;
   }

   public FancyMessage translationReplacements(Iterable<FancyMessage> replacements) {
      return this.translationReplacements(ArrayWrapper.toArray(replacements, FancyMessage.class));
   }

   public FancyMessage then(String text) {
      return this.then(TextualComponent.rawText(text));
   }

   private FancyMessage then(TextualComponent text) {
      if (!this.latest().hasText()) {
         throw new IllegalStateException("previous message part has no text");
      } else {
         this.messageParts.add(new MessagePart(text));
         this.dirty = true;
         return this;
      }
   }

   public FancyMessage then() {
      if (!this.latest().hasText()) {
         throw new IllegalStateException("previous message part has no text");
      } else {
         this.messageParts.add(new MessagePart());
         this.dirty = true;
         return this;
      }
   }

   @Override
   public void writeJson(JsonWriter writer) throws IOException {
      if (this.messageParts.size() == 1) {
         this.latest().writeJson(writer);
      } else {
         writer.beginObject().name("text").value("").name("extra").beginArray();

         for (MessagePart part : this) {
            part.writeJson(writer);
         }

         writer.endArray().endObject();
      }
   }

   private String toJSONString() {
      if (!this.dirty && this.jsonString != null) {
         return this.jsonString;
      } else {
         StringWriter string = new StringWriter();
         JsonWriter json = new JsonWriter(string);

         try {
            this.writeJson(json);
            json.close();
         } catch (IOException var4) {
            throw new RuntimeException("invalid message");
         }

         this.jsonString = string.toString();
         this.dirty = false;
         return this.jsonString;
      }
   }

   public void send(Player player) {
      this.send(player, this.toJSONString());
   }

   private void send(CommandSender sender, String jsonString) {
      if (!(sender instanceof Player player)) {
         sender.sendMessage(this.toOldMessageFormat());
      } else {
         player.sendMessage(net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(jsonString));
      }
   }

   public void send(CommandSender sender) {
      this.send(sender, this.toJSONString());
   }

   public void send(Iterable<? extends CommandSender> senders) {
      String string = this.toJSONString();

      for (CommandSender sender : senders) {
         this.send(sender, string);
      }
   }

   private String toOldMessageFormat() {
      StringBuilder result = new StringBuilder();

      for (MessagePart part : this) {
         result.append(part.color == null ? "" : part.color);

         for (ChatColor formatSpecifier : part.styles) {
            result.append(formatSpecifier);
         }

         result.append(part.text);
      }

      return result.toString();
   }

   private MessagePart latest() {
      return this.messageParts.get(this.messageParts.size() - 1);
   }

   private void onClick(String name, String data) {
      MessagePart latest = this.latest();
      latest.clickActionName = name;
      latest.clickActionData = data;
      this.dirty = true;
   }

   private void onHover(String name, JsonRepresentedObject data) {
      MessagePart latest = this.latest();
      latest.hoverActionName = name;
      latest.hoverActionData = data;
      this.dirty = true;
   }

   public Map<String, Object> serialize() {
      HashMap<String, Object> map = new HashMap<>();
      map.put("messageParts", this.messageParts);
      return map;
   }

   @Override
   public Iterator<MessagePart> iterator() {
      return this.messageParts.iterator();
   }

   static {
      ConfigurationSerialization.registerClass(FancyMessage.class);
   }
}
