#!/usr/bin/ruby -w

require "json"
require "open3"

$github_token = ENV["GITHUB_TOKEN"]
if $github_token.nil?
  begin
    $github_token = File.read("#{ENV['HOME']}/.github-token").chomp
  rescue StandardError
    nil
  end
end
if $github_token.nil?
  STDERR.puts "Could not find Github token. Tried GITHUB_TOKEN environment variable and " +
                  "#{ENV['HOME']}/.github-token"
  exit 1
end

$app_name = ENV.fetch("PROJECT") {|_|
  STDERR.puts "PROJECET not set."
  exit 1
}
$branch = ENV.fetch("BRANCH") {|_|
  STDERR.puts "BRANCH not set."
  exit 1
}
$commit_sha = ENV.fetch("GIT_SHA") {|_|
  STDERR.puts "GIT_SHA not set."
  exit 1
}

github_api_url_root = "https://api.github.com/repos/broadinstitute/firecloud-develop"

def call_github(url, *extra_curl_params)
  curl_cmd = [
      "curl", "-f",
      "-H", "Authorization: token #{$github_token}",
      *extra_curl_params,
      "#{url}"
  ]
  Open3.popen3(*curl_cmd) {|stdin, stdout, stderr, wait_thread|
    if wait_thread.value.success?
      yield JSON.load(stdout)
    else
      STDERR.puts "curl call failed:"
      STDERR.puts curl_cmd.inspect
      STDERR.puts stderr.read
      exit 1
    end
  }
end

def dump(x)
  puts JSON.pretty_generate(x)
end

url = "#{github_api_url_root}/git/refs/heads/#{$branch}"
latest_commit_url, latest_commit_sha = call_github(url) {|response|
  o = response["object"]
  [o["url"], o["sha"]]
}

tree_sha = call_github(latest_commit_url) {|response|
  response["tree"]["sha"]
}

new_tree_input = {
    :base_tree => tree_sha,
    :tree => [{
                  :path => $app_name,
                  :mode => "160000",
                  :type => "commit",
                  :sha => $commit_sha
              }]
}
url = "#{github_api_url_root}/git/trees"
new_tree_sha = call_github(url, "-d", new_tree_input.to_json) {|response|
  response["sha"]
}

new_commit_input = {
    :message => "Bump #{$app_name} version to #{$commit_sha}",
    :parents => [latest_commit_sha],
    :tree => new_tree_sha
}
url = "#{github_api_url_root}/git/commits"
new_commit_sha = call_github(url, "-d", new_commit_input.to_json) {|response|
  response["sha"]
}

update_branch_input = {
    :sha => new_commit_sha
}
url = "#{github_api_url_root}/git/refs/heads/#{$branch}"
call_github(url, "-d", update_branch_input.to_json) {|response|
  puts "#{$branch} updated."
}
